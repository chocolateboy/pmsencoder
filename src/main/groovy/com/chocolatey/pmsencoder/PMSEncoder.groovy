@Typed
package com.chocolatey.pmsencoder

// if these extend Exception (rather than RuntimeException) Groovy(++?) wraps them in
// InvokerInvocationException, which causes all kinds of tedium.
// For the time being: extend RuntimeException - even though they're both checked

public class StopMatchingException extends RuntimeException { }

public class PMSEncoderConfigException extends RuntimeException {
    // what's with the "cannot find constructor" errors? A GString thing? (again?!)
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
 * Input parameters are stored in the Stash object's key/value pairs, specifically the EXECUTABLE, URI and OUTPUT
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
    // e.g. new Command([ URI: uri ])
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
        log.info("matching URI: ${command.stash['URI']}")

        profiles.each { name, profile ->
            log.info("trying profile: $name");

            if (profile.match(command)) {
                log.info("success")
                matched << name
            } else {
                log.info("failure")
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
        // run the profile block at compile-time but store its pattern and action blocks
        // for execution at runtime
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

class ProfileBlockDelegate {
    public Closure patternBlock = null
    public Closure actionBlock = null
    private String name

    ProfileBlockDelegate(String name) {
        this.name = name
        assert this.actionBlock == null
        assert this.patternBlock == null
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

        // we made it without triggering an exception, so the two fields are sane: extract them
        this.patternBlock = delegate.patternBlock
        this.actionBlock = delegate.actionBlock
    }

    // pulled out of the match method below so that type-softening is isolated
    // note keep it here rather than making it a method in PatternBlockDelegate: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runPatternBlock(PatternBlockDelegate delegate) {
        // match methods short-circuit matching on failure by throwing a StopMatchingException,
        // so we need to wrap this in a try catch block

        try {
            delegate.with(patternBlock)
        } catch (StopMatchingException e) {
            log.debug("pattern block: caught match exception")
            // one of the match methods failed, so the whole match failed
            return false
        }

        // success simply means "no match failure exception was thrown" - this also handles cases where the
        // pattern block is empty.
        log.debug("pattern block: matched OK")
        return true
    }

    // pulled out of the match method below so that type-softening is isolated
    // note keep it here rather than making it a method in ActionBlockDelegate: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runActionBlock(ActionBlockDelegate delegate) {
        delegate.with(actionBlock)
    }

    boolean match(Command command) {
        def newCommand = new Command(command) // clone() doesn't work with Groovy++
        def patternBlockDelegate = new PatternBlockDelegate(command)

        // returns true if all matches in the block succeed, false otherwise 
        if (runPatternBlock(patternBlockDelegate)) {
            log.info("matched $name")
            // we can now merge any side-effects (currently only modifications to the stash)
            // first instantiate the ActionBlockDelegate so that we can call its let helper method
            // to log stash merges
            def actionBlockDelegate = new ActionBlockDelegate(command, config)
            // now merge
            newCommand.stash.each { name, value -> actionBlockDelegate.let(newCommand.stash, name, value) }
            // now run the actions
            runActionBlock(actionBlockDelegate)
            return true
        } else {
            return false
        }
    }
}

// TODO: add isGreaterThan (gt?), isLessThan (lt?), and equals (eq?) matchers?
class PatternBlockDelegate extends Logger {
    private static final StopMatchingException STOP_MATCHING = new StopMatchingException()
    private final Command command

    PatternBlockDelegate(Command command) {
        this.command = command
    }

    // DSL method
    void match(Map<String, String> map) {
        map.each { name, value -> match(name, value) }
    }

    // DSL method
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
}

/* XXX: add configurable HTTP proxy support? */
class ActionBlockDelegate extends Logger {
    private final Config config
    private final Command command
    private final Map<String, String> cache = [:]

    @Lazy private HTTPClient http = new HTTPClient()

    ActionBlockDelegate(Command command, Config config) {
        this.command = command
        this.config = config
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

    // not a DSL method: do the heavy-lifting of stash assignment.
    // public because Profile needs to call it (after a successful match)
    void let(Stash stash, String name, String value) {
        if ((stash[name] == null) || (stash[name] != value)) {
            String[] new_value = [ value ] // FIXME can't get Reference to work transparently here
            log.info("setting \$$name to $value")
         
            stash.each { stash_key, stash_value ->
                /*
                    TODO do this natively i.e. interpret the string
                    as a GString (with bindings in "stash") rather than performing the
                    interpolation manually. this would also allow the string to contain
                    arbitrary groovy expressions e.g.

                        pattern {
                            match uri: '^http://www\\.example\\.com/(?<id>\\d+)\\.html'
                        }

                        action {
                            let uri: 'http://www.example.com/${id + 1}.html'
                        }
                */

                def var_name_regex = ~/(?:(?:\$$stash_key\b)|(?:\$\{$stash_key\}))/
         
                if (new_value[0] =~ var_name_regex) {
                    log.info("replacing \$$stash_key with '${stash_value.replaceAll("'", "\\'")}' in ${new_value[0]}")
                    // XXX squashed bug: strings are immutable!
                    new_value[0] = new_value[0].replaceAll(var_name_regex, stash_value)
                }
            }

            stash[name] = new_value[0]
            log.info("set \$$name to ${new_value[0]}")
        }
    }

    /*
        1) get the URI pointed to by stash[uri] (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update the stash with any named captures
    */
    // DSL method
    void scrape(String regex) {
        def stash = command.stash
        def uri = stash['URI']
        def document = cache[uri]
        def newStash = new Stash()

        assert newStash != null

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

    // define a variable in the stash, performing any variable substitutions
    // DSL method
    void let(Map<String, String> map) {
        map.each { key, value ->
            let(command.stash, key, value)
        }
    }

    void set(Map<String, String> map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> set(name, value) }
    }

    // set an MEncoder option - create it if it doesn't exist
    // DSL method
    void set(String name, String value = null) {
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
    void youtube(List<String> formats = config.YOUTUBE_ACCEPT) {
        def stash = command.stash
        def uri = stash['URI']
        def video_id = stash['video_id']
        def t = stash['t']
        def found = false

        assert video_id != null
        assert t != null

        if (formats.size() > 0) {
            found = formats.any { fmt ->
                def stream_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$video_id&t=$t&asv="
                log.info("trying fmt $fmt: $stream_uri")

                if (http.head(stream_uri)) {
                    log.info("success")
                    // set the new URI - note: use the low-level interface NOT the (deferred) DSL interface!
                    let(stash, 'URI', stream_uri)
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
            log.info("setting args [ $index .. ${index + andFollowing} ] to $replaceList")
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
