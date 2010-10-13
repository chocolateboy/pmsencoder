@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

// if these extend Exception (rather than RuntimeException) Groovy(++?) wraps them in
// InvokerInvocationException, which causes all kinds of tedium.
// For the time being: extend RuntimeException - even though they're both checked

public class MatchFailureException extends RuntimeException { }

public class PMSEncoderException extends RuntimeException {
    // grrr: get "cannot find constructor" errors without this (another GString issue?)
    PMSEncoderException(String msg) {
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

    private java.lang.String canonicalize(Object key) {
        java.lang.String name = key.toString()
        name.startsWith('$') ? name : '$' + name
    }

    public Stash(Map<String, String> map) {
        map.each { key, value -> this.put(key.toString(), value.toString()) }
    }

    public java.lang.String put(java.lang.String key, java.lang.String value) {
        super.put(canonicalize(key), value)
    }

    public java.lang.String put(Object key, Object value) {
        super.put(canonicalize(key), value.toString())
    }

    public java.lang.String get(java.lang.String key) {
        super.get(canonicalize(key))
    }

    public java.lang.String get(Object key) {
        super.get(canonicalize(key))
    }
}

/*
 * this object encapsulates the input/output parameters passed from/to the PMS transcode launcher (Engine.java).
 * Input parameters are stored in the Stash object's key/value pairs, specifically the executable, uri and output
 * fields; and the output (a command) is returned via the same stash (i.e. the executable and URI can be overridden)
 * as well as in a list of strings accessible via the args field
 */
public class Command extends Logger {
    Stash stash
    List<String> args
    List<String> matches

    private Command(Stash stash, List<String> args, List<String> matches) {
        this.stash = stash
        this.args = args
        this.matches = matches
    }

    public Command() {
        this(new Stash(), [], [])
    }

    public Command(Stash stash) {
        this(stash, [])
    }

    public Command(List<String> args) {
        this(new Stash(), args)
    }

    public Command(Stash stash, List<String> args) {
        this(stash, args, [])
    }

    // convenience constructor: allow the stash to be supplied as a Map<String, String>
    // e.g. new Command([ uri: uri ])
    public Command(Map<String, String> map) {
        this(new Stash(map), [], [])
    }

    public Command(Command other) {
        this(new Stash(other.stash), new ArrayList<String>(other.args), new ArrayList<String>(other.matches))
    }

    public boolean equals(Command other) {
        this.stash == other.stash && this.args == other.args && this.matches == other.matches
    }

    public java.lang.String toString() {
        "{ stash: $stash, args: $args, matches: $matches }".toString()
    }

    // setter implementation with logged stash assignments
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    public String let(String name, String value) {
        if ((stash[name] == null) || (stash[name] != value.toString())) {
            log.info("setting $name to $value")
            stash[name] = value
        }

        return value // for chaining: foo = bar = baz i.e. foo = (bar = baz)
    }
}

class Matcher extends Logger {
    // FIXME: this is only public for a (single) test
    // 1) Config has the same scope as Matcher so they could be merged,
    // but we don't want to expose load
    // 2) the config "globals" (e.g. $DEFAULT_MENCODER_PATH) could be moved here
    public final Config config

    Matcher(PMS pms) {
        this.config = new Config(pms)
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

    @Typed(TypePolicy.DYNAMIC) // XXX needed to handle GStrings
    boolean match(Command command, boolean useDefault = true) {
        if (useDefault) {
            // watch out: there's a GString about
            config.$DEFAULT_MENCODER_ARGS.each { command.args << it.toString() }
        }

        def matched = config.match(command) // we could use the @Delegate annotation, but this is cleaner/clearer

        if (matched) {
            log.debug("command: $command")
        }

        return matched
    }
}

class Config extends Logger {
    private Map<String, Profile> profiles = [:] // defaults to LinkedHashMap

    // DSL fields (mutable)
    public List<String> $DEFAULT_MENCODER_ARGS = []
    public List<Integer> $YOUTUBE_ACCEPT = []
    public PMS $PMS

    public Config(PMS pms) {
        $PMS = pms
    }

    boolean match(Command command) {
        log.info("matching URI: ${command.stash['$URI']}")

        // XXX make sure we take the name from the profile itself
        // rather than the map key - the latter may have been usurped
        // by a profile with a different name

        profiles.values().each { profile ->
            if (profile.match(command)) {
                command.matches << profile.name
            }
        }

        return command.matches.size() > 0
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
    // XXX more annoying DDWIM magic: Groovy reorders the arguments
    // http://enfranchisedmind.com/blog/posts/groovy-argument-reordering/
    protected void profile (Map<String, String> options = [:], String name, Closure closure) throws PMSEncoderException {
        // run the profile block at compile-time to extract its pattern and action blocks,
        // but invoke them at runtime
        String extendz = options['extends']
        String overrides = options['overrides']

        if ((profiles[name] != null)) {
            log.info("replacing profile: $overrides with $name")
        } else {
            log.info("registering profile: $name")
        }

        Profile profile = new Profile(name, this)

        // TODO need detailed diagnostics
        try {
            profile.extractBlocks(closure)

            if (extendz) {
                profile.assignPatternBlockIfNull(profiles[extendz])
                profile.assignActionBlockIfNull(profiles[extendz])
            }

            // this is why name is defined both as the key of the map and in the profile
            // itself. the key is only used for ordering/replacement
            // the public name is always the profile's own name field
            if (overrides) {
                profiles[overrides] = profile
            } else {
                profiles[name] = profile
            }
        } catch (Throwable e) {
            log.error("invalid profile ($name): " + e.getMessage())
        }
    }
}

// this holds a reference to the pattern and action, and isn't delegated to at runtime
class Profile extends Logger {
    private final Config config
    protected Closure patternBlock
    protected Closure actionBlock

    public final String name

    Profile(String name, Config config) {
        this.name = name
        this.config = config
    }

    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void extractBlocks(Closure closure) {
        def delegate = new ProfileValidationDelegate(config, name)
        // wrapper method: runs the closure then validates the result, raising an exception if anything is amiss
        delegate.runProfileBlock(closure)

        // we made it without triggering an exception, so the two fields are sane: save them
        this.patternBlock = delegate.patternBlock // possibly null
        this.actionBlock = delegate.actionBlock   // possibly null
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Pattern: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runPatternBlock(Pattern delegate) {
        if (patternBlock == null) {
            // unconditionally match
            log.debug("no pattern block supplied: matched OK")
        } else {
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
            // pattern block is empty
            log.debug("pattern block: matched OK")
        }

        return true
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Action: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    boolean runActionBlock(Action delegate) {
        if (actionBlock != null) {
            delegate.with(actionBlock)
        } else {
            return true
        }
    }

    boolean match(Command command) {
        def newCommand = new Command(command)
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
            // first merge (with logging)
            newCommand.stash.each { name, value -> command.let(name, value) }
            // now run the actions
            def action = new Action(config, command)
            runActionBlock(action)
            return true
        } else {
            return false
        }
    }

    public void assignPatternBlockIfNull(Profile profile) {
        // XXX can't get ?= to work here...
        if (this.patternBlock == null) {
            this.patternBlock = profile.patternBlock
        }
    }

    public void assignActionBlockIfNull(Profile profile) {
        // XXX can't get ?= to work here...
        if (this.actionBlock == null) {
            this.actionBlock = profile.actionBlock
        }
    }
}

// i.e. a delegate with access to a Config
public class ConfigDelegate extends Logger {
    private Config config

    public ConfigDelegate(Config config) {
        this.config = config
    }

    // DSL properties

    // $CONFIG: read-only
    protected final Config get$CONFIG() {
        config
    }

    // $PMS: read-only
    protected final PMS get$PMS() {
        config.$PMS
    }

    // DSL getter: $DEFAULT_MENCODER_ARGS
    protected List<String> get$DEFAULT_MENCODER_ARGS() {
        config.$DEFAULT_MENCODER_ARGS
    }

    // DSL setter: $DEFAULT_MENCODER_ARGS
    protected List<String> get$DEFAULT_MENCODER_ARGS(List<String> args) {
        config.$DEFAULT_MENCODER_ARGS = args
    }

    // DSL getter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT() {
        config.$YOUTUBE_ACCEPT
    }

    // DSL setter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT(List<String> args) {
        config.$YOUTUBE_ACCEPT = args
    }
}

// i.e. a delegate with access to a Command
public class CommandDelegate extends ConfigDelegate {
    private Command command

    public CommandDelegate(Config config, Command command) {
        super(config)
        this.command = command
    }

    // DSL properties

    // $COMMAND: read-only
    protected Command get$COMMAND() {
        command
    }

    // $STASH: read-only
    protected final Stash get$STASH() {
        command.stash
    }

    // DSL accessor ($ARGS): read-only
    protected List<String> get$ARGS() {
        command.args
    }

    // DSL accessor ($ARGS): read-write
    @Typed(TypePolicy.DYNAMIC) // try to handle GStrings
    // FIXME: test this!
    protected List<String> set$ARGS(List<String> args) {
        command.args = args.collect { it.toString() } // handle GStrings
    }

    // DSL accessor ($MATCHES): read-only
    protected List<String> get$MATCHES() {
        command.matches
    }

    // DSL getter
    protected String propertyMissing(String name) throws PMSEncoderException {
        command.stash[name]
    }

    // DSL setter
    protected String propertyMissing(String name, Object value) {
        command.let(name, value.toString())
    }
}

class ProfileValidationDelegate extends ConfigDelegate {
    public Closure patternBlock = null
    public Closure actionBlock = null
    public String name

    ProfileValidationDelegate(Config config, String name) {
        super(config)
        this.name = name
    }

    // DSL method

    private void pattern(Closure closure) throws PMSEncoderException {
        if (this.patternBlock == null) {
            this.patternBlock = closure
        } else {
            throw new PMSEncoderException("invalid profile ($name): multiple pattern blocks defined")
        }
    }

    // DSL method
    private void action(Closure closure) throws PMSEncoderException {
        if (this.actionBlock == null) {
            this.actionBlock = closure
        } else {
            throw new PMSEncoderException("invalid profile ($name): multiple action blocks defined")
        }
    }

    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    private runProfileBlock(Closure closure) {
        this.with(closure)
        // the pattern block is optional; if not supplied, the profile always matches
        // the action block is optional; if not supplied no action is performed
    }
}

class Pattern extends CommandDelegate {
    private static final MatchFailureException STOP_MATCHING = new MatchFailureException()

    Pattern(Config config, Command command) {
        super(config, command)
    }

    // DSL setter - overrides the CommandDelegate method to avoid logging,
    // which is handled later (if the match succeeds) by merging the pattern
    // block's temporary stash
    protected String propertyMissing(String name, Object value) {
        $STASH[name] = value
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domain(Map<String, String> map) {
        map.each { name, value -> domain(name, value) }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domain(String name, String value) {
        if (!matchString(name, domainToRegex(value.toString()))) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domain(String name, List<String> values) {
        if (!(values.any { value -> matchString(name, domainToRegex(value.toString())) })) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(Map<String, String> map) {
        map.each { name, value -> match(name, value) }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name, List<String> values) {
        if (!(values.any { value -> matchString(name.toString(), value.toString()) })) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name, String value) {
        if (!matchString(name.toString(), value.toString())) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(Closure closure) {
        log.info("running match block")

        if (closure()) {
            log.info("success")
        } else {
            log.info("failure")
            throw STOP_MATCHING
        }
    }

    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    private boolean matchString(String name, String value) {
        assert (name != null) && (value != null)

        if ($STASH[name] == null) { // this will happen for old custom configs that use let uri: ...
            log.warn("invalid match: $name is not defined")
            // fall through
        } else {
            log.info("matching $name against $value")

            if (RegexHelper.match($STASH[name], value, $STASH)) {
                log.info("success")
                return true // abort default failure below
            } else {
                log.info("failure")
            }
        }

        return false
    }

    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    private String domainToRegex(String domain) {
        return "^https?://(\\w+\\.)*${domain}/".toString()
    }
}

/* XXX: add configurable HTTP proxy support? */
class Action extends CommandDelegate {
    private final Map<String, String> cache = [:]

    @Lazy private HTTPClient http = new HTTPClient()

    Action(Config config, Command command) {
        super(config, command)
    }

   /*
        1) get the URI pointed to by options['uri'] or stash['$URI'] (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update the stash with any named captures
    */
    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void scrape(String regex, Map<String, String> options = [:]) {
        def uri = options['uri'] ?: $STASH['$URI']
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
            newStash.each { name, value -> $COMMAND.let(name, value) }
        } else {
            log.info("failure")
        }
    }

    // define a variable in the stash
    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void let(Map<String, String> map) {
        map.each { key, value ->
            $COMMAND.let(key, value)
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void set(Map<String, String> map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> setArg(name.toString(), value.toString()) }
    }

    // set an MEncoder option - create it if it doesn't exist
    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void setArg(String name, String value = null) {
        assert name != null

        def index = $ARGS.findIndexOf { it == name }

        if (index == -1) {
            if (value != null) {
                log.info("adding $name $value")
                /*
                    XXX squashed bug: careful not to perform operations on $STASH or $COMMAND.args
                    that return and subsequenly operate on a new value
                    (i.e. make sure they're modified in place):

                        def args = $COMMAND.args
                        args += ... // XXX doesn't modify $COMMAND.args
                */
                $ARGS << name << value
            } else {
                log.info("adding $name")
                $ARGS << name
            }
        } else if (value != null) {
            log.info("setting $name to $value")
            $ARGS[ index + 1 ] = value
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
            // squashed bug (see  above): take care to $ARGS in-place
            def index = $ARGS.findIndexOf { it == name }

            if (index != -1) {
                map.each { search, replace ->
                    log.info("replacing $search with $replace in $name")
                    // TODO support named captures
                    def value = $ARGS[ index + 1 ]

                    if (value) {
                        // XXX bugfix: strings are immutable!
                        $ARGS[ index + 1 ] = value.replaceAll(search, replace)
                    }
                }
            }
        }
    }

    /*
        given (in the stash) the $youtube_video_id and $youtube_t values of a YouTube stream URI
        (i.e. the direct link to a video), construct the full URI with various $fmt values in
        succession and set the stash $URI value to the first one that's valid (based on a HEAD request)
    */

    // DSL method
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    void youtube(List<Integer> formats = $YOUTUBE_ACCEPT) {
        def uri = $STASH['$URI']
        def video_id = $STASH['$youtube_video_id']
        def t = $STASH['$youtube_t']
        def found = false

        assert video_id != null
        assert t != null

        $COMMAND.let('$youtube_uri', uri)

        if (formats.size() > 0) {
            found = formats.any { fmt ->
                def stream_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$video_id&t=$t&asv="
                log.info("trying fmt $fmt: $stream_uri")

                if (http.head(stream_uri)) {
                    log.info("success")
                    // set the new URI - note: use the low-level interface NOT the (deferred) DSL interface!
                    $COMMAND.let('$URI', stream_uri)
                    $COMMAND.let('$youtube_fmt', fmt)
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
        $COMMAND.args += args
    }

    // DSL method: prepend a list of options to the command's args list
    void prepend(List<String> args) {
        $COMMAND.args = args + $ARGS
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
        splice($ARGS, optionName, [], andFollowing)
    }

    // DSL method: replace an option (and optionally the following n arguments) with the supplied arg list
    void replace(String optionName, List<String> replaceList, int andFollowing = 0) {
        splice($ARGS, optionName, replaceList, andFollowing)
    }
}
