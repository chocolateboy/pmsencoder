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
    @Lazy private HTTPClient http = new HTTPClient()
    // this is the default Map type, but let's be explicit as we strictly need this type
    private Map<String, Profile> profiles = new LinkedHashMap<String, Profile>()
    private PMS pms
    private List<String> mencoder = []
    private List<String> mplayer = []
    private List<String> ffmpeg = []
    private List<String> ffmpegOut = []
    private List<Integer> youtubeAccept = []
    private Map<String, Object> stash = new HashMap<String, Object>()
    PMSConf pmsConf = new PMSConf()

    Matcher(PMS pms) {
        this.pms = pms
    }

    boolean match(Command command, boolean useDefault = true) {
        if (useDefault) {
            command.transcoder = ffmpeg*.toString()
            command.output = ffmpegOut*.toString()
        }

        def uri = command.stash.get('$URI')
        log.debug("matching URI: $uri")

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
            log.trace("command: $command")
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
            log.info("replacing profile $replaces with: $name")
        } else {
            target = name
            if (profiles[name] != null) {
                log.info("replacing profile: $name")
            } else {
                log.info("registering ${stage.toString().toLowerCase()} profile: $name")
            }
        }

        def profile = new Profile(this, name, stage)

        try {
            // run the profile block at compile-time to extract its (optional) pattern and action blocks,
            // but invoke them at runtime
            profile.extractBlocks(closure)

            if (extendz != null) {
                if (profiles[extendz] == null) {
                    log.error("attempt to extend a nonexistent profile: $extendz")
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
            log.error("invalid profile ($name): " + e.getMessage())
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

        groovy.evaluate(reader, filename)
    }

    void loadUserScripts(File scriptDirectory) {
        if (!scriptDirectory.isDirectory()) {
            log.error("invalid user script directory ($scriptDirectory): not a directory")
        } else if (!scriptDirectory.exists()) {
            log.error("invalid user script directory ($scriptDirectory): directory doesn't exist")
        } else {
            log.info("loading user scripts from: $scriptDirectory")
            scriptDirectory.eachFileRecurse(FILES) { File file ->
                def filename = file.getName()
                if (filename.endsWith('.groovy')) {
                    log.info("loading user script: $filename")
                    try {
                        load(file)
                    } catch (Exception e) {
                        def path = file.getAbsolutePath()
                        log.error("can't load user script: $path", e)
                    }
                }
            }
        }
    }

    private URL getResource(String name) {
        return this.getClass().getResource("/$name");
    }

    void loadDefaultScripts() {
        log.info('loading built-in scripts')

        getResource('lib.txt').eachLine() { String scriptName ->
            log.info("loading built-in script: $scriptName")
            def scriptURL = getResource(scriptName)
            if (scriptURL == null) {
                log.error("can't load $scriptURL")
            } else {
                load(scriptURL)
            }
        }
    }

    Object propertyMissing(String name) {
        log.trace("retrieving global variable: $name")
        return stash[name]
    }

    Object propertyMissing(String name, Object value) {
        log.info("setting global variable: $name = $value")
        return stash[name] = value
    }

    protected Object getVar(String name) {
        stash[name]
    }

    protected boolean hasVar(String name) {
        stash.containsKey(name)
    }

    protected Object setVar(String name, Object value) {
        stash[name] = value
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

    // $HTTP: getter
    public HTTPClient get$HTTP() {
        http
    }

    // $PMS: getter
    public final PMS get$PMS() {
        pms
    }

    // DSL getter: $MENCODER
    public List<String> get$MENCODER() {
        mencoder
    }

    // DSL setter: $MENCODER
    public List<String> set$MENCODER(Object stringOrList) {
        mencoder = Util.stringList(stringOrList)
    }

    // DSL getter: $MPLAYER
    public List<String> get$MPLAYER() {
        mplayer
    }

    // DSL setter: $MPLAYER
    public List<String> set$MPLAYER(Object stringOrList) {
        mplayer = Util.stringList(stringOrList)
    }

    // DSL getter: $FFMPEG
    public List<String> get$FFMPEG() {
        ffmpeg
    }

    // DSL setter: $FFMPEG
    public List<String> set$FFMPEG(Object stringOrList) {
        ffmpeg = Util.stringList(stringOrList)
    }

    // DSL getter: $FFMPEG_OUT
    public List<String> get$FFMPEG_OUT() {
        ffmpegOut
    }

    // DSL setter: $FFMPEG_OUT
    public List<String> set$FFMPEG_OUT(Object stringOrList) {
        ffmpegOut = Util.stringList(stringOrList)
    }

    // DSL getter: $YOUTUBE_ACCEPT
    public List<Integer> get$YOUTUBE_ACCEPT() {
        youtubeAccept
    }

    // DSL setter: $YOUTUBE_ACCEPT
    public List<Integer> set$YOUTUBE_ACCEPT(List<Integer> args) {
        youtubeAccept = args
    }

    // $OS: getter
    public final String get$OS() {
        System.getProperty('os.name')
    }
}
