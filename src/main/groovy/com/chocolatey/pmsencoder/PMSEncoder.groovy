@Typed
package com.chocolatey.pmsencoder

// class PMSEncoderException extends RuntimeException { }

interface SubAction {
    void call(Command command, ActionState state)
}

interface SubPattern {
    boolean call(Command command)
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

    public void assign(Command other) {
        this.stash = other.stash
        this.args = other.args
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

// common state shared across a sequence of subactions
class ActionState {
    public final Map<String, String> cache = [:]
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
            config.MENCODER_ARGS.each { command.args << it }
        }

        config.match(command) // we could use the @Delegate annotation, but this is cleaner/clearer
    }
}

class Config extends Logger {
    private final Map<String, Profile> profiles = [:] // defaults to LinkedHashMap

    // DSL fields (mutable)
    public List<String> MENCODER_ARGS = []
    public List<Integer> YOUTUBE_ACCEPT = []

    List<String> match(Command command) {
        List<String> matched = []
        log.info("matching URI: ${command.stash['URI']}")
        profiles.values().each { profile ->
            Closure subactions

            if ((subactions = profile.match(command))) {
                subactions()
                matched << profile.name
            }
        }

        return matched
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void config(Closure closure) {
        this.with(closure)
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void profile (String name, Closure closure) {
        Profile profile = new Profile(name, this)
        profile.with(closure)
        profiles[name] = profile
    }
}

class Profile extends Logger {
    private final Config config
    private Pattern pattern
    private Action action

    public final String name

    Profile(String name, Config config) {
        this.name = name
        this.config = config
    }

    Closure match(Command command) {
        // make sure this uses LinkedHashMap (the Groovy default Map implementation)
        // to ensure predictable iteration ordering (e.g. for tests)
        Command newCommand = new Command(command) // clone() doesn't work with Groovy++
        assert newCommand != null

        if (pattern.match(newCommand)) {
            /*
                return a closure that encapsulates all the side-effects of a successful
                match e.g.
                
                1) log the name of the matched profile
                2) perform any side effects of the match (e.g. merge any bindings that were created/modified
                   by calls to the DSL's match method)
                3) execute the profile's corresponding action
            */
            return {
                log.info("matched $name")
                // merge all the name/value bindings resulting from the match
                command.assign(newCommand)
                action.execute(command)
                return name
            }
        } else {
            return null
        }
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void pattern(Closure closure) {
        Pattern pattern = new Pattern(config)
        pattern.with(closure)
        this.pattern = pattern
    }

    // DSL method
    @Typed(TypePolicy.MIXED) // Groovy++ doesn't support delegation
    void action (Closure closure) {
        Action action = new Action(config)
        action.with(closure)
        this.action = action
    }
}

// TODO: add isGreaterThan (gt?), isLessThan (lt?), and equals (eq?) matchers?
class Pattern {
    private final Config config
    private final List<SubPattern> subpatterns = []

    Pattern(Config config) {
        this.config = config
    }

    // DSL method
    void match(Map<String, String> map) {
        map.each { name, value -> match(name, value) }
    }

    // DSL method
    void match(String name, String value) {
        subpatterns << { command ->
            assert command != null
            RegexHelper.match(command.stash[name], value, command.stash)
        }
    }

    boolean match(Command command) {
        subpatterns.every { subpattern -> subpattern(command) }
    }
}

/* XXX: add configurable HTTP proxy support? */
class Action extends Logger {
    private final Config config
    private final List<SubAction> subactions = []

    @Lazy private URLDecoder decoder = new URLDecoder() 
    @Lazy private HTTPClient http = new HTTPClient()

    Action(Config config) {
        this.config = config
    }

    // DSL properties

    // args
    protected final List<String> getMENCODER_ARGS() {
        config.MENCODER_ARGS
    }

    // YOUTUBE_ACCEPT
    protected final List<String> getYOUTUBE_ACCEPT() {
        config.YOUTUBE_ACCEPT
    }

    void execute(Command command) {
        ActionState state = new ActionState()
        subactions.each { subaction -> subaction(command, state) }
    }

    // not a DSL method: do the heavy-lifting of stash assignment.
    // public because Profile needs to call it
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
        subactions << { command, state ->
            def stash = command.stash
            def uri = stash['URI']
            def document = state.cache[uri]
            def newStash = new Stash()

            assert newStash != null

            if (!document) {
                log.info("getting $uri")
                document = state.cache[uri] = http.get(uri)
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
    }

    // define a variable in the stash, performing any variable substitutions
    // DSL method
    void let(Map<String, String> map) {
        map.each { key, value ->
            subactions << { command, state -> let(command.stash, key, value) }
        }
    }

    void set(Map<String, String> map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> set(name, value) }
    }

    // set an MEncoder option - create it if it doesn't exist
    // DSL method
    void set(String name, String value = null) {
        subactions << { command, state ->
            log.info("inside set: $name => $value")
            def args = command.args
            def index = args.findIndexOf { it == name }
         
            if (index == -1) {
                if (value != null) {
                    log.info("adding $name $value")
                    /*
                        XXX squashed bug - we need to modify stash and args in-place,
                        which means we can't use:
                        
                            args += ...

                        - or indeeed anything that returns a new value of args/stash

                        that's mildly inconvenient, but nowhere near as inconvenient as having to
                        thread args/stash through every call/return from Match and Action
                        closures
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
    }

    /*
        perform a search-and-replace in the value of an MEncoder option
        TODO: signature: handle null, a single map and an array of maps
    */
    // DSL method
    void tr(Map<String, Map<String, String>> replaceMap) {
        subactions << { command, state ->
            // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
            replaceMap.each { name, map ->
                def args = command.args
                def index = args.findIndexOf { it == name }
             
                if (index != -1) {
                    map.each { search, replace ->
                        log.info("replacing $search with $replace")
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
    }

    /*
        given (in the stash) the $video_id and $t values of a YouTube stream URI (i.e. the direct link to a video),
        construct the full URI with various $fmt values in succession and set the stash $uri value to the first one
        that's valid (based on a HEAD request)
    */

    // DSL method
    void youtube(List<String> formats = config.YOUTUBE_ACCEPT) {
        subactions << { command, state ->
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
    }

    // DSL method: append a list of options to thge command's args list
    void append(List<String> args) {
        subactions << { command, state ->
            command.args += args
        }
    }

    // DSL method: prepend a list of options to thge command's args list
    void prepend(List<String> args) {
        subactions << { command, state ->
            command.args = args + command.args
        }
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
        subactions << { command, state ->
            splice(command.args, optionName, [], andFollowing)
        }
    }

    // DSL method: replace an option (and optionally the following n arguments) with the supplied arg list
    void replace(String optionName, List<String> replaceList, int andFollowing = 0) {
        subactions << { command, state ->
            splice(command.args, optionName, replaceList, andFollowing)
        }
    }
}
