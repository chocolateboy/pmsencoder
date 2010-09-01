@Typed
package com.chocolatey.pmsencoder

// if these extend Exception (rather than RuntimeException) Groovy(++?) wraps them in
// InvokerInvocationException, which causes all kinds of tedium.
// For the time being: extend RuntimeException - even though they're both checked

public class MatchFailureException extends RuntimeException { }

public class PMSEncoderConfigException extends RuntimeException {
    // grrr: get "cannot find constructor" errors without this (another GString issue?)
    PMSEncoderConfigException(String msg) {
        super(msg)
    }
}

// a long-winded way of getting Java Strings and Groovy GStrings to play nice
public class Stash extends LinkedHashMap<java.lang.String, java.lang.String> {
    public Stash() {
        super()
    }

    public Stash(Stash old) {
        super()
        old.each { key, value -> this.put(key.toString(), value.toString()) }
    }

    public Stash(Map<String, String> map) {
        map.each { key, value -> this.put(key.toString(), value.toString()) }
    }

    public java.lang.String put(Object key, Object value) {
        super.put(key.toString(), value.toString())
    }

    public java.lang.String get(Object key) {
        super.get(key.toString())
    }
}

/*
 * this object encapsulates the input/output parameters passed from/to the PMS transcode launcher (Engine.java).
 * Input parameters are stored in the Stash object's key/value pairs, specifically the executable, uri and output
 * fields; and the output (a command) is returned via the same stash (i.e. the executable and URI can be overridden)
 * as well as in a list of strings accessible via the args field
 */
public class Command {
    Stash stash
    List<String> args

    public Command() {
        this.stash = new Stash()
        this.args = []
    }

    // copy constructor (clone doesn't work in Groovy++)
    public Command(Command old) {
        this.stash = new Stash(old.stash)
        this.args = new ArrayList<String>(old.args)
    }

    public boolean equals(Command other) {
        this.stash == other.stash && this.args == other.args
    }

    public Command(Stash stash) {
        this.stash = stash
        this.args = []
    }

    // convenience constructor: allow the stash to be supplied as a Map<String, String>
    // e.g. new Command([ uri: uri ])
    public Command(Map<String, String> map) {
        this.stash = new Stash(map)
        this.args = []
    }

    public Command(List<String> args) {
        this.stash = new Stash()
        this.args = args
    }

    public Command(Stash stash, List<String> args) {
        this.stash = stash
        this.args = args
    }

    public java.lang.String toString() {
        "{ stash: $stash, args: $args }".toString()
    }
}

class Matcher extends Logger {
    private final Config config

    Matcher() {
        this.config = new Config()
    }

    void load(String path) {
        load(new File(path))
    }

    void load(File file) {
        load(new FileInputStream(file))
    }

    void load(URL url) {
        load(url.openStream())
    }

    void load(InputStream stream) {
        load(new InputStreamReader(stream))
    }

    void load(Reader reader) {
        // XXX squashed bug: don't drain the reader by logging its value!
        Script script = new GroovyShell().parse(reader)
        ExpandoMetaClass emc = new ExpandoMetaClass(script.class, false)

        emc.setProperty('config', config.&config) // set the DSL's sole top-level method: config
        emc.initialize()
        script.metaClass = emc
        script.run()
    }

    List<String> match(Command command, boolean useDefault = true) {
        if (useDefault) {
            config.DEFAULT_MENCODER_ARGS.each { command.args << it }
        }

        config.match(command) // we could use the @Delegate annotation, but this is cleaner/clearer
    }
}

class Config extends Logger {
    private final Map<String, Profile> profiles = [:] // defaults to LinkedHashMap

    // DSL fields (mutable)
    public List<String> DEFAULT_MENCODER_ARGS = []
    public List<Integer> YOUTUBE_ACCEPT = []

    List<String> match(Command command) {
        List<String> matched = []
        log.info("matching URI: ${command.stash['uri']}")

        profiles.each { name, profile ->
            if (profile.match(command)) {
                matched << name
            }
        }

        return matched
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void config(Closure closure) {
        this.with(closure) // run at (config file) compile-time
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    // a Profile consists of a name, a pattern block and an action block - all
    // determined when the config file is loaded/compiled
    void profile (String name, Closure closure) {
        // run the profile block at compile-time to extract its pattern and action blocks,
        // but invoke those at runtime
        log.info("registering profile: $name")
        Profile profile = new Profile(name, this)

        try {
            profile.extractBlocks(closure)
            profiles[name] = profile
        } catch (Throwable e) {
            log.error("invalid profile ($name): " + e.getMessage());
        }
    }
}

class Profile extends Logger {
    private final Config config
    private Closure patternBlock
    private Closure actionBlock

    public final String name

    Profile(String name, Config config) {
        this.name = name
        this.config = config
    }

    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void extractBlocks(Closure closure) {
        def delegate = new ProfileBlockDelegate(name)
        // wrapper method: runs the closure then validates the result, raising an exception if anything is amiss
        delegate.runProfileBlock(closure)

        // we made it without triggering an exception, so the two fields are sane: save them
        this.patternBlock = delegate.patternBlock
        this.actionBlock = delegate.actionBlock
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Pattern: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runPatternBlock(Pattern delegate) {
        // pattern methods short-circuit matching on failure by throwing a MatchFailureException,
        // so we need to wrap this in a try/catch block

        try {
            delegate.with(patternBlock)
        } catch (MatchFailureException e) {
            log.debug("pattern block: caught match exception")
            // one of the match methods failed, so the whole block failed
            return false
        }

        // success simply means "no match failure exception was thrown" - this also handles cases where the
        // pattern block is empty.
        log.debug("pattern block: matched OK")
        return true
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Action: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runActionBlock(Action delegate) {
        delegate.with(actionBlock)
    }

    boolean match(Command command) {
        def newCommand = new Command(command) // clone() doesn't work with Groovy++
        // the pattern block has its own command object (which is initially the same as the action block's).
        // if the match succeeds, then the pattern block's stash is merged into the action block's stash.
        // this ensures that a partial match (i.e. a failed match) with side-effects/bindings doesn't contaminate
        // the action, and, more importantly, it defers logging until the whole pattern block has
        // completed successfully
        def pattern = new Pattern(config, newCommand)

        log.info("matching $name")

        // returns true if all matches in the block succeed, false otherwise 
        if (runPatternBlock(pattern)) {
            // we can now merge any side-effects (currently only modifications to the stash).
            // first instantiate the Action so that we can call its let() helper method
            // to perform and log stash merges
            def action = new Action(config, command)
            // now merge
            newCommand.stash.each { name, value -> action.let(command.stash, name, value) }
            // now run the actions
            runActionBlock(action)
            return true
        } else {
            return false
        }
    }
}

public class BaseDelegate extends Logger {
    public Config config
    public Command command

    public BaseDelegate(Config config, Command command) {
        this.config = config
        this.command = command
    }
        
    protected List<String> getArgs() {
        command.args
    }

    protected List<String> setArgs(List<String> args) {
        command.args = args
    }

    // DSL properties

    // DEFAULT_MENCODER_ARGS
    protected final List<String> getDEFAULT_MENCODER_ARGS() {
        config.DEFAULT_MENCODER_ARGS
    }

    // YOUTUBE_ACCEPT
    protected final List<String> getYOUTUBE_ACCEPT() {
        config.YOUTUBE_ACCEPT
    }

    // DSL getter
    protected String propertyMissing(String name) {
        command.stash[name]
    }
}

class ProfileBlockDelegate {
    public Closure patternBlock = null
    public Closure actionBlock = null
    public String name

    ProfileBlockDelegate(String name) {
        this.name = name
    }

    // DSL method
    private void pattern(Closure closure) throws PMSEncoderConfigException {
        if (this.patternBlock == null) {
            this.patternBlock = closure
        } else {
            throw new PMSEncoderConfigException("invalid profile ($name): multiple pattern blocks defined")
        }
    }

    // DSL method
    private void action(Closure closure) throws PMSEncoderConfigException {
        if (this.actionBlock == null) {
            this.actionBlock = closure
        } else {
            throw new PMSEncoderConfigException("invalid profile ($name): multiple action blocks defined")
        }
    }

    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    private runProfileBlock(Closure closure) throws PMSEncoderConfigException {
        this.with(closure)

        if (patternBlock == null) {
            throw new PMSEncoderConfigException("invalid profile ($name): no pattern block defined")
        }

        if (actionBlock == null) {
            throw new PMSEncoderConfigException("invalid profile ($name): no action block defined")
        }
    }
}

// TODO: add gt (greater than) and lt (less than)?
class Pattern extends BaseDelegate {
    private static final MatchFailureException STOP_MATCHING = new MatchFailureException()

    Pattern(Config config, Command command) {
        super(config, command)
    }

    // DSL setter - overrides the BaseDelegate method to avoid logging,
    // which is handled later (if the match succeeds) by merging the pattern
    // block's temporary stash
    protected String propertyMissing(String name, Object value) {
        command.stash[name] = value
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(Map<String, String> map) {
        map.each { name, value -> match(name, value) }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void eq(Map<String, String> map) {
        map.each { name, value -> eq(name, value) }
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void match(String name, String value) {
        assert name && value

        if (command.stash[name] == null) { // this will happen for old custom configs that use let uri: ...
            log.warn("invalid match: $name is not defined")
            // fall through
        } else {
            log.info("matching $name against $value")

            if (RegexHelper.match(command.stash[name], value, command.stash)) {
                log.info("success")
                return // abort default failure exception below
            } else {
                log.info("failure")
            }
        }

        throw STOP_MATCHING
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void eq(String name, String value) {
        if (command.stash[name] == null) { // this will happen for old custom configs that use let uri: ...
            log.warn("invalid eq: $name is not defined")
            // fall through
        } else {
            log.info("eq: checking $name (${command.stash[name]}) against $value")

            if (command.stash[name] == value.toString()) {
                log.info("success")
                return
            }
        }

        log.info("failure")
        throw STOP_MATCHING
    }
}

/* XXX: add configurable HTTP proxy support? */
class Action extends BaseDelegate {
    private final Map<String, String> cache = [:]

    @Lazy private HTTPClient http = new HTTPClient()

    Action(Config config, Command command) {
        super(config, command)
    }

    // DSL setter
    protected String propertyMissing(String name, Object value) {
        let(command.stash, name, value.toString())
    }

    // log stash assignments
    // public because Profile needs to call it (after a successful match)
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    public String let(Stash stash, String name, String value) {
        if ((stash[name] == null) || (stash[name] != value.toString())) {
            log.info("setting \$$name to $value")
            stash[name] = value
        }

        return value
    }
 
    /*
        1) get the URI pointed to by stash[uri] (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update the stash with any named captures
    */
    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void scrape(String regex) {
        def stash = command.stash
        def uri = stash['uri']
        def document = cache[uri]
        def newStash = new Stash()

        if (document == null) {
            log.info("getting $uri")
            document = cache[uri] = http.get(uri)
        }

        assert document != null

        log.info("matching content of $uri against $regex")

        if (RegexHelper.match(document, regex, newStash)) {
            log.info("success")
            newStash.each { name, value -> let(stash, name, value) }
        } else {
            log.info("failure")
        }
    }

    // define a variable in the stash
    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void let(Map<String, String> map) {
        map.each { key, value ->
            let(command.stash, key, value)
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void set(Map<String, String> map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> setArg(name, value) }
    }

    // set an MEncoder option - create it if it doesn't exist
    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void setArg(String name, String value = null) {
        assert name != null

        def args = command.args
        def index = args.findIndexOf { it == name }
     
        if (index == -1) {
            if (value != null) {
                log.info("adding $name $value")
                /*
                    XXX squashed bug:
                    
                    unless we want to fully qualify command members each time (command.args = ...)
                    or reassign them at the end of the method (command.stash = newStash),
                    we need to modify stash and args in-place, i.e. we can't use:
                    
                        args += ...

                    - or indeeed anything that returns a new value of args/stash
                */
                args << name << value
            } else {
                log.info("adding $name")
                args << name // FIXME: encapsulate args handling
            }
        } else if (value != null) {
            log.info("setting $name to $value")
            args[ index + 1 ] = value // FIXME: encapsulate args handling
        }
    }

    /*
        perform a search-and-replace in the value of an MEncoder option
        TODO: signature: handle null, a single map and an array of maps
    */

    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void tr(Map<String, Map<String, String>> replaceMap) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        replaceMap.each { name, map ->
            def args = command.args
            def index = args.findIndexOf { it == name }
         
            if (index != -1) {
                map.each { search, replace ->
                    log.info("replacing $search with $replace in $name")
                    // TODO support named captures
                    // FIXME: encapsulate args handling
                    def value = args[ index + 1 ]

                    if (value) {
                        // XXX bugfix: strings are immutable!
                        args[ index + 1 ] = value.replaceAll(search, replace)
                    }
                }
            }
        }
    }

    /*
        given (in the stash) the $video_id and $t values of a YouTube stream URI (i.e. the direct link to a video),
        construct the full URI with various $fmt values in succession and set the stash $uri value to the first one
        that's valid (based on a HEAD request)
    */

    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void youtube(List<Integer> formats = config.YOUTUBE_ACCEPT) {
        def stash = command.stash
        def uri = stash['uri']
        def video_id = stash['youtube_video_id']
        def t = stash['youtube_t']
        def found = false

        assert video_id != null
        assert t != null

        let(stash, 'youtube_uri', uri)

        if (formats.size() > 0) {
            found = formats.any { fmt ->
                def stream_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$video_id&t=$t&asv="
                log.info("trying fmt $fmt: $stream_uri")

                if (http.head(stream_uri)) {
                    log.info("success")
                    // set the new URI - note: use the low-level interface NOT the (deferred) DSL interface!
                    let(stash, 'uri', stream_uri)
                    let(stash, 'youtube_fmt', fmt)
                    return true
                } else {
                    log.info("failure")
                    return false
                }
            }
        } else {
            log.fatal("no formats defined for $uri")
        }

        if (!found) {
            log.fatal("can't retrieve stream URI for $uri")
        }
    }

    // DSL method: append a list of options to the command's args list
    void append(List<String> args) {
        command.args += args
    }

    // DSL method: prepend a list of options to the command's args list
    void prepend(List<String> args) {
        command.args = args + command.args
    }

    // private helper method containing code common to replace and remove
    private boolean splice(List<String> args, String optionName, List<String> replaceList, int andFollowing = 0) {
        def index = args.indexOf(optionName)

        if (index == -1) {
            log.warn("invalid splice: can't find $optionName in $args")
            return false
        } else {
            log.info("setting ${args[ index .. (index + andFollowing) ]} to $replaceList")
            args[ index .. (index + andFollowing) ] = replaceList
            return true
        }
    }

    // DSL method: remove an option (and optionally the following n arguments) from the command's arg list
    void remove(String optionName, int andFollowing = 0) {
        splice(command.args, optionName, [], andFollowing)
    }

    // DSL method: replace an option (and optionally the following n arguments) with the supplied arg list
    void replace(String optionName, List<String> replaceList, int andFollowing = 0) {
        splice(command.args, optionName, replaceList, andFollowing)
    }
}
