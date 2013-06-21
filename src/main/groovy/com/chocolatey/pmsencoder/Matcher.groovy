package com.chocolatey.pmsencoder

import static groovy.io.FileType.FILES

import net.pms.PMS
import net.pms.configuration.PmsConfiguration

import org.jsoup.nodes.Document

enum Stage { BEGIN, INIT, SCRIPT, CHECK, END }

// no need to extend HashMap<...>: we only need the subscript - i.e. getAt() and putAt() - syntax
@Singleton
@groovy.transform.CompileStatic
class PMSConf {
    @Lazy private PmsConfiguration configuration = PMS.getConfiguration()

    public Object getAt(String key) {
        return configuration.getCustomProperty(key?.toString())
    }

    // XXX squashed bug: putAt, *not* setAt -- only recently fixed in the Groovy documentation...
    public void putAt(String key, Object value) {
        configuration.setCustomProperty(key?.toString(), value)
    }
}

// XXX note: only public methods can be delegated to
@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
class Matcher {
    // XXX work around Groovy fail: getHttp goes into an infinite loop if this is lazy
    private HTTPClient http = new HTTPClient()
    // this is the default Map type, but let's be explicit as we strictly need this type
    private Map<String, Profile> profiles = new LinkedHashMap<String, Profile>()
    private boolean isSortProfiles = false
    private List<Profile> sortedProfiles
    private PMS pms
    private List<String> ffmpeg = []
    private List<Integer> youtubeAccept = []
    private Map<String, Object> globals = new Stash()
    private PMSConf pmsConf = PMSConf.getInstance()

    // global caches
    Map<String, Boolean> youTubeDLCache = [:]
    Map<String, Boolean> getFlashVideosCache = [:]

    Matcher(PMS pms) {
        this.pms = pms
    }

    // sort the profiles by a) stage b) document order within
    // a script (which is preserved in the unsorted list)
    public void sortProfiles() {
        List<Profile> unsortedProfiles = profiles.values().asList()

        sortedProfiles = unsortedProfiles.sort { Profile profile1, Profile profile2 ->
            // XXX Groovy truth (0 == false) doesn't work under CompileStatic
            def cmp = profile1.stage.compareTo(profile2.stage)
            if (cmp != 0) {
                return cmp
            } else {
                return unsortedProfiles.indexOf(profile1).compareTo(unsortedProfiles.indexOf(profile2))
            }
        }

        isSortProfiles = false
    }

    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    boolean match(Command command, boolean useDefault = true) {
        synchronized(isSortProfiles) {
            if (isSortProfiles) {
                sortProfiles()
            }
        }

        if (useDefault) {
            command.transcoder = ffmpeg*.toString()
        }

        def uri = command.getVarAsString('uri')
        logger.debug("matching URI: $uri")
        boolean stopMatching = false

        sortedProfiles.each { Profile profile ->
            if (stopMatching == false || profile.alwaysRun == true) {
                if (profile.match(command)) {
                    /*
                        XXX make sure we take the name from the profile itself
                        rather than the Map key - the latter may have been
                        usurped by a profile with a different name e.g.

                            profile ('Foo', replaces: 'Bar')
                    */
                    command.matches << profile.name

                    if (profile.stopOnMatch) {
                        stopMatching = true
                    }
                }
            }
        }

        def matched = command.matches.size() > 0
        if (matched) {
            logger.debug("command: $command")
        }

        return matched
    }

    // DSL method
    // a Profile consists of a name, a pattern block and an action block - all
    // determined when the script is loaded/compiled
    public void registerProfile(String name, Stage stage, Map<String, Object> options, Closure closure) {
        Boolean stopOnMatch = options.containsKey('stopOnMatch') ? options['stopOnMatch'] : true
        Boolean alwaysRun = options.containsKey('alwaysRun') ? options['alwaysRun'] : false
        String extendz = options['extends']?.toString()
        String replaces = options['replaces']?.toString()

        def target

        if (replaces != null) {
            target = replaces
            logger.info("replacing profile $replaces with: $name")
        } else {
            target = name
            if (profiles[name] != null) {
                logger.info("replacing profile: $name")
            } else {
                if (options) {
                    logger.info("registering ${stage.toString().toLowerCase()} profile: $name ($options)")
                } else {
                    logger.info("registering ${stage.toString().toLowerCase()} profile: $name")
                }
            }
        }

        def profile = new Profile(this, name, stage, stopOnMatch, alwaysRun)

        try {
            // run the profile block at compile-time to extract its (optional) pattern and action blocks,
            // but invoke them at runtime
            profile.extractBlocks(closure)

            if (extendz != null) {
                if (profiles[extendz] == null) {
                    logger.error("attempt to extend a nonexistent profile: $extendz")
                } else {
                    def base = profiles[extendz]
                    profile.assignPatternBlockIfNull(base)
                    profile.assignActionBlockIfNull(base)
                }
            }

            // this is why name is defined both as the key of the map and in the profile
            // itself. the key allows replacement
            profiles[target] = profile
        } catch (Throwable e) {
            logger.error("invalid profile ($name): " + e.getMessage())
        }
    }

    void load(String path, String filename = path) {
        load(new File(path), filename)
    }

    void load(URL url, String filename = url.toString()) {
        load(url.openStream(), filename)
    }

    void load(File file, String filename = file.getPath()) {
        load(new FileInputStream(file), filename)
    }

    void load(InputStream stream, String filename) {
        load(new InputStreamReader(stream), filename)
    }

    // we could impose a constraint here that a script (file) must
    // contain exactly one script block, but why bother?
    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    void load(Reader reader, String filename) {
        // we'll typically be adding new profiles, so notify match() to
        // recalculate the list of sorted profiles when we're done
        isSortProfiles = true

        def binding = new Binding(
            'begin':  this.&begin,
            'init':   this.&init,
            'script': this.&script,
            'check':  this.&check,
            'end':    this.&end
        )

        def groovy = new GroovyShell(binding)

        // the file (or URL) basename (e.g. foo for foo.groovy) determines the classname
        // (e.g. foo.class). this can cause problems if a userscript has a name that conflicts
        // with a DSL identifier e.g. hook.groovy ("you tried to assign a ... to a class").
        // we can work around this by prefixing each class name with a poor-man's namespace
        // i.e. an arbitrary string which distinguishes classes from identifiers e.g.:
        // hook.groovy -> pmsencoder_script_hook.class
        filename = filename.replaceAll('(\\w+\\.groovy)$', 'pmsencoder_script_$1')
        groovy.evaluate(reader, filename)
    }

    void loadUserScripts(File scriptDirectory) {
        if (!scriptDirectory.exists()) {
            logger.error("invalid user script directory ($scriptDirectory): directory doesn't exist")
        } else if (!scriptDirectory.isDirectory()) {
            logger.error("invalid user script directory ($scriptDirectory): not a directory")
        } else {
            logger.info("loading user scripts from: $scriptDirectory")
            scriptDirectory.eachFileRecurse(FILES) { File file ->
                def filename = file.getName()
                if (filename.endsWith('.groovy')) {
                    logger.info("loading user script: $filename")
                    try {
                        load(file)
                    } catch (Exception e) {
                        def path = file.getAbsolutePath()
                        logger.error("can't load user script: $path", e)
                    }
                }
            }
        }
    }

    private URL getResource(String name) {
        return this.getClass().getResource("/$name");
    }

    void loadDefaultScripts() {
        logger.info('loading built-in scripts')

        getResource('lib.txt').eachLine() { String scriptName ->
            logger.info("loading built-in script: $scriptName")
            def scriptURL = getResource(scriptName)
            if (scriptURL == null) {
                logger.error("can't load $scriptURL")
            } else {
                load(scriptURL)
            }
        }
    }

    Object propertyMissing(String name) {
        logger.trace("retrieving global variable: $name")
        return globals.get(name)
    }

    Object propertyMissing(String name, Object value) {
        logger.info("setting global variable: $name = $value")
        return globals.put(name, value)
    }

    protected Object getVar(String name) {
        globals.get(name)
    }

    protected boolean hasVar(String name) {
        globals.containsKey(name)
    }

    protected String setVar(String name, String value) {
        globals.put(name, value)
    }

    // DSL method
    protected void begin(Closure closure) {
        closure.delegate = new Script(this, Stage.BEGIN)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    // DSL method
    protected void init(Closure closure) {
        closure.delegate = new Script(this, Stage.INIT)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    // DSL method
    protected void script(Closure closure) {
        closure.delegate = new Script(this, Stage.SCRIPT)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    // DSL method
    protected void check(Closure closure) {
        closure.delegate = new Script(this, Stage.CHECK)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    // DSL method
    protected void end(Closure closure) {
        closure.delegate = new Script(this, Stage.END)
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    // DSL properties

    // http: getter
    public HTTPClient getHttp() {
        this.http
    }

    // pms: getter
    public final PMS getPms() {
        this.pms
    }

    // DSL getter: FFMPEG
    public List<String> getFFMPEG() {
        this.ffmpeg
    }

    // DSL setter: FFMPEG
    public List<String> setFFMPEG(Object maybeList) {
        this.ffmpeg = Util.toStringList(maybeList, true) // true: split on whitespace if it's a String
    }

    // DSL getter: YOUTUBE_ACCEPT
    public List<Integer> getYOUTUBE_ACCEPT() {
        this.youtubeAccept
    }

    // DSL setter: YOUTUBE_ACCEPT
    public List<Integer> setYOUTUBE_ACCEPT(List<Integer> args) {
        this.youtubeAccept = args
    }

    // os: getter
    public final String getOs() {
        System.getProperty('os.name')
    }

    // pmsConf: getter
    public final PMSConf getPmsConf() {
        this.pmsConf
    }
}
