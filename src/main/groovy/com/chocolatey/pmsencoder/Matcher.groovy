package com.chocolatey.pmsencoder

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j
import net.pms.PMS
import net.pms.configuration.PmsConfiguration
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

import static groovy.io.FileType.FILES

enum Stage { BEGIN, INIT, DEFAULT, CHECK, END }

// no need to extend HashMap<...>: we only need the subscript - i.e. getAt() and putAt() - syntax
@Singleton
@CompileStatic
class PMSConf {
    private final PmsConfiguration configuration = PMS.getConfiguration()

    public Object getAt(String key) {
        return configuration.getCustomProperty(key?.toString())
    }

    // XXX squashed bug: putAt, *not* setAt -- only recently fixed in the Groovy documentation...
    public void putAt(String key, Object value) {
        configuration.setCustomProperty(key?.toString(), value)
    }
}

// XXX note: only public methods can be delegated to
@CompileStatic
@Log4j(value="logger")
class Matcher {
    // XXX work around Groovy fail: getHttp goes into an infinite loop if this is lazy
    private final HTTPClient http = new HTTPClient()
    // this is the default Map type, but let's be explicit as we strictly need this type
    private final Map<String, Profile> profiles = new LinkedHashMap<String, Profile>()
    private boolean collateProfiles = false
    private final Map<Event, List<Profile>> eventProfiles = [:].withDefault { [] }
    private static final PMS pms = PMS.get()
    private List<String> ffmpeg = []
    private List<Integer> youtubeAccept = []
    private final Map<String, Object> globals = new Stash()
    private final PMSConf pmsConf = PMSConf.getInstance()

    // "global" (i.e. per-Matcher) caches used by ProfileDelegate.isYouTubeDLCompatible
    // and ProfileDelegate.isGetFlashVideosCompatible
    final Map<String, Boolean> youTubeDLCache = [:]
    final Map<String, Boolean> getFlashVideosCache = [:]

    static {
        // make String.match(pattern) (i.e. RegexHelper.match(string, pattern))
        // available to scripts
        installExtensionMethods()
    }

    Matcher() { }

    // install extension methods: (G)String.match(pattern) -> RegexHelper.match(delegate, pattern).
    // the obvious place to put this is in Plugin, but the test suite doesn't load that class.
    // FIXME these extensions are global: we want to restrict them to scripts
    // FIXME the META-INF way doesn't work: from the Maven output:
    //
    //     WARNING: Module [pmsencoder-extensions] - Unable to load extension class [com.chocolatey.pmsencoder.StringExtension]
    @CompileStatic(TypeCheckingMode.SKIP)
    static private void installExtensionMethods() {
        String.metaClass {
            match { Object regex -> RegexHelper.match(delegate, regex) }
        }

        GString.metaClass {
            match { Object regex -> RegexHelper.match(delegate, regex) }
        }
    }

    /*
     * 1) sort the profiles by a) stage b) document order within
     * a script (which is preserved in the unsorted list).
     *
     * 2) iterate over the profiles and store them in eventProfiles,
     * which maps each event to a sorted list of profiles
     * that consume that event e.g.:
     *
     *     [
     *         TRANSCODE: [ profile1, profile2           ] ],
     *         FINALIZE:  [ profile3, profile4, profile5 ] ],
     *         ...
     *     [
     */
    private void collateProfiles() {
        def unsortedProfiles = profiles.values().asList()

        def sortedProfiles = unsortedProfiles.sort { Profile profile1, Profile profile2 ->
            def cmp = profile1.stage.compareTo(profile2.stage)

            // XXX Groovy truth (0 == false) doesn't work here under CompileStatic
            if (cmp != 0) {
                return cmp
            } else {
                return unsortedProfiles.indexOf(profile1).compareTo(unsortedProfiles.indexOf(profile2))
            }
        }

        eventProfiles.clear()
        sortedProfiles.each { Profile profile ->
            eventProfiles[profile.event] << profile
        }
    }

    // called by Plugin.match to control logging
    boolean consumesEvent(Event event) {
        // XXX squashed bug: make sure eventProfiles has
        // been properly (re-)initialised before checking
        // for consumers
        checkCollateProfiles()
        eventProfiles[event].size() > 0
    }

    private synchronized void checkCollateProfiles() {
        if (collateProfiles) {
            collateProfiles()
            collateProfiles = false
        }
    }

    boolean match(Command command, boolean useDefaultTranscoder = true) {
        def uri = command.getVarAsString('uri')

        checkCollateProfiles()

        def sortedProfiles = eventProfiles[command.event]

        // bypass matching altogether if no profiles
        // consume this event
        // XXX hmm, Groovy truth ([] == false) seems to work here...
        if (!sortedProfiles) {
            return false
        }

        if (useDefaultTranscoder) {
            // XXX the spread operator breaks CompileStatic:
            //
            //     java.lang.VerifyError: (class: com/chocolatey/pmsencoder/Matcher,
            //     method: match signature: (Lcom/chocolatey/pmsencoder/Command;Z)Z)
            //     Incompatible object argument for function call
            //
            // possibly related: https://jira.codehaus.org/browse/GROOVY-6311
            //
            // command.transcoder = ffmpeg*.toString()
            command.transcoder = Util.toStringList(ffmpeg) // clone
        }

        def stopMatching = false

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

        return command.matches.size() > 0
    }

    // DSL method
    // a Profile consists of a name, a map of options, a pattern block
    // and an action block - all determined when the script is loaded/compiled
    public void registerProfile(String name, Stage stage, Map<String, Object> options, Closure closure) {
        Boolean stopOnMatch = options.containsKey('stopOnMatch') ? options['stopOnMatch'] : true
        Boolean alwaysRun = options.containsKey('alwaysRun') ? options['alwaysRun'] : false
        String extendz = options['extends']?.toString()
        String replaces = options['replaces']?.toString()
        Event event = (options['on'] != null) ? (options['on'] as Event) : Event.TRANSCODE

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
                    logger.info("registering ${stage} profile: $name ($options)")
                } else {
                    logger.info("registering ${stage} profile: $name")
                }
            }
        }

        def profile = new Profile(this, name, stage, stopOnMatch, alwaysRun, event)

        try {
            // run the profile block at compile time to extract its (optional)
            // pattern and action blocks, but invoke them at runtime
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

            // we've added (or modified) a profile, which means we'll need to
            // (re-)collate them when match() is called
            collateProfiles = true
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
    // contain exactly one script block, but why do that?
    void load(Reader reader, String filename) {
        def binding = new Binding([
            script: this.&script
        ])

        // https://groovy.codeplex.com/wikipage?title=Guillaume%20Laforge%27s%20%22Mars%20Rover%22%20tutorial%20on%20Groovy%20DSL%27s
        // XXX these should be static fields (they don't change) but Groovy
        // throws unhelpful errors if we try to configure them in a static initializer
        // XXX ditto if we configure them in the constructor.
        def CompilerConfiguration compilerConfiguration = new CompilerConfiguration()
        def ImportCustomizer imports = new ImportCustomizer()

        // XXX CompileStatic error if these enum members are accessed as properties e.g.:
        //
        //     imports.addStaticStars(Stage.name, Event.name)
        //
        // TODO file a bug for this
        imports.addStaticStars(Stage.getName(), Event.getName())
        compilerConfiguration.addCompilationCustomizers(imports)

        def groovy = new GroovyShell(binding, compilerConfiguration)

        // the file (or URL) basename (e.g. 'foo' for foo.groovy) determines the classname
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
        logger.info("setting global variable: $name = ${value.inspect()}")
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
    // spell these out (no default parameters) to work around Groovy bugs
    protected void script(Closure closure) {
        script(Stage.DEFAULT, closure)
    }

    // DSL method
    protected void script(Stage stage, Closure closure) {
        closure.delegate = new ScriptDelegate(this, stage)
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
