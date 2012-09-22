@Typed
package com.chocolatey.pmsencoder

import static groovy.io.FileType.FILES

import net.pms.PMS

enum Stage { BEGIN, INIT, SCRIPT, CHECK, END }

class PMSConf { // no need to extend HashMap<...>: we only need the subscript - i.e. getAt() - syntax
    public Object getAt(String key) {
        return PMS.getConfiguration().getCustomProperty(key?.toString())
    }
}

// XXX note: only public methods can be delegated to
class Matcher implements LoggerMixin {
    // XXX work around Groovy fail: getHttp goes into an infinite loop if this is lazy
    private HTTPClient http = new HTTPClient()
    // this is the default Map type, but let's be explicit as we strictly need this type
    private Map<String, Profile> profiles = new LinkedHashMap<String, Profile>()
    private PMS pms
    private List<String> mencoder = []
    private List<String> mplayer = []
    private List<String> ffmpeg = []
    private List<String> ffmpegOut = []
    private List<Integer> youtubeAccept = []
    private Map<String, Object> globals = new HashMap<String, Object>()
    PMSConf pmsConf = new PMSConf()

    Matcher(PMS pms) {
        this.pms = pms
    }

    boolean match(Command command, boolean useDefault = true) {
        if (useDefault) {
            command.transcoder = ffmpeg*.toString()
            command.output = ffmpegOut*.toString()
        }

        def uri = command.getVar('uri')
        logger.debug("matching URI: $uri")

        // XXX this is horribly inefficient, but it's a) trivial to implement and b) has the right semantics
        // the small number of scripts make this a non-issue for now
        Stage.each { stage ->
            profiles.values().each { profile ->
                if (profile.stage == stage && profile.match(command)) {
                    // XXX make sure we take the name from the profile itself
                    // rather than the Map key - the latter may have been usurped
                    // by a profile with a different name
                    command.matches << profile.name
                }
            }
        }

        def matched = command.matches.size() > 0

        if (matched) {
            logger.trace("command: $command")
        }

        return matched
    }

    // DSL method
    // a Profile consists of a name, a pattern block and an action block - all
    // determined when the script is loaded/compiled
    public void registerProfile(String name, Stage stage, Map<String, String> options, Closure closure) {
        def extendz = options['extends']
        def replaces = options['replaces']
        def target

        if (replaces != null) {
            target = replaces
            logger.info("replacing profile $replaces with: $name")
        } else {
            target = name
            if (profiles[name] != null) {
                logger.info("replacing profile: $name")
            } else {
                logger.info("registering ${stage.toString().toLowerCase()} profile: $name")
            }
        }

        def profile = new Profile(this, name, stage)

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
    void load(Reader reader, String filename) {
        def binding = new Binding(
            begin:  this.&begin,
            init:   this.&init,
            script: this.&script,
            check:  this.&check,
            end:    this.&end
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
        if (!scriptDirectory.isDirectory()) {
            logger.error("invalid user script directory ($scriptDirectory): not a directory")
        } else if (!scriptDirectory.exists()) {
            logger.error("invalid user script directory ($scriptDirectory): directory doesn't exist")
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

    protected String getVar(String name) {
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

    // DSL getter: MENCODER
    public List<String> getMENCODER() {
        this.mencoder
    }

    // DSL setter: MENCODER
    public List<String> setMENCODER(Object stringOrList) {
        this.mencoder = Util.stringList(stringOrList)
    }

    // DSL getter: MPLAYER
    public List<String> getMPLAYER() {
        this.mplayer
    }

    // DSL setter: MPLAYER
    public List<String> setMPLAYER(Object stringOrList) {
        this.mplayer = Util.stringList(stringOrList)
    }

    // DSL getter: FFMPEG
    public List<String> getFFMPEG() {
        this.ffmpeg
    }

    // DSL setter: FFMPEG
    public List<String> setFFMPEG(Object stringOrList) {
        this.ffmpeg = Util.stringList(stringOrList)
    }

    // DSL getter: FFMPEG_OUT
    public List<String> getFFMPEG_OUT() {
        this.ffmpegOut
    }

    // DSL setter: FFMPEG_OUT
    public List<String> setFFMPEG_OUT(Object stringOrList) {
        this.ffmpegOut = Util.stringList(stringOrList)
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
}
