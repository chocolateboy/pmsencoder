@Typed
package com.chocolatey.pmsencoder

// class PMSEncoderException extends RuntimeException { }

interface SubAction {
    void call(Stash stash, List<String> args, ActionState state)
}

interface SubPattern {
    boolean call(Stash stash, List<String> args)
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

    public java.lang.String put(Object key, Object value) {
        super.put(key.toString(), value.toString())
    }

    public java.lang.String get(Object key) {
        super.get(key.toString())
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

    List<String> match(Stash stash, List<String> args, boolean useDefault = true) {
        if (useDefault) {
            config.MENCODER_ARGS.each { args << it }
        }

        config.match(stash, args) // we could use the @Delegate annotation, but this is cleaner/clearer
    }
}

class Config extends Logger {
    private final Map<String, Profile> profiles = [:] // defaults to LinkedHashMap

    // DSL fields (mutable)
    public List<String> MENCODER_ARGS = []
    public List<Integer> YOUTUBE_ACCEPT = []

    List<String> match(Stash stash, List<String> args) {
        List<String> matched = []
        log.info("matching URI: ${stash['uri']}")
        profiles.values().each { profile ->
            Closure subactions

            if ((subactions = profile.match(stash, args))) {
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

    Closure match(Stash stash, List<String> args) {
        // make sure this uses LinkedHashMap (the Groovy default Map implementation)
        // to ensure predictable iteration ordering (e.g. for tests)
        Stash new_stash = new Stash(stash) // clone() doesn't work with Groovy++
        assert new_stash != null

        if (pattern.match(new_stash, args)) {
            /*
                return a closure that encapsulates all the side-effects of a successful
                match e.g.
                
                1) log the name of the matched profile
                2) perform any side effects of the match (e.g. bind any variables
                   that were extracted by calls to the DSL's match method)
                3) execute the profile's corresponding action
            */
            return {
                log.info("matched $name")
                // merge all the name/value bindings resulting from the match
                new_stash.each { name, value -> action.let(stash, name, value) }
                action.execute(stash, args)
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
        subpatterns << { stash, args ->
            assert stash != null
            RegexHelper.match(stash[name], value, stash)
        }
    }

    boolean match(Stash stash, List<String> args) {
        subpatterns.every { pattern -> pattern(stash, args) }
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

    void execute(Stash stash, List<String> args) {
        ActionState state = new ActionState()
        subactions.each { subaction -> subaction(stash, args, state) }
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
        subactions << { stash, args, state ->
            def uri = stash['uri']
            def document = state.cache[uri]
            def new_stash = new Stash()

            assert new_stash != null

            if (!document) {
                log.info("getting $uri")
                document = state.cache[uri] = http.get(uri)
            }

            assert document != null

            log.info("matching content of $uri against $regex")

            if (RegexHelper.match(document, regex, new_stash)) {
                log.info("success")
                new_stash.each { name, value -> let(stash, name, value) }
            } else {
                log.info("failure")
            }
        }
    }

    // define a variable in the stash, performing any variable substitutions
    // DSL method
    void let(Map<String, String> map) {
        map.each { key, value ->
            subactions << { stash, args, state -> let(stash, key, value) }
        }
    }

    void set(Map<String, String> map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> set(name, value) }
    }

    // set an mencoder option - create it if it doesn't exist
    // DSL method
    void set(String name, String value = null) {
        subactions << { stash, args, state ->
            log.info("inside set: $name => $value")
            def qname = "-$name"
            def index = args.findIndexOf { it == qname }
         
            if (index == -1) {
                if (value) {
                    log.info("adding $qname $value")
                    /*
                        XXX squashed bug - we need to modify stash and args in-place,
                        which means we can't use:
                        
                            args += ...

                        - or indeeed anything that returns a new value of args/stash

                        that's mildly inconvenient, but nowhere near as inconvenient as having to
                        thread args/stash through every call/return from Match and Action
                        closures
                    */
                    args << qname << value
                } else {
                    log.info("adding $qname")
                    args << qname // FIXME: encapsulate args handling
                }
            } else if (value) {
                log.info("setting $qname to $value")
                args[ index + 1 ] = value // FIXME: encapsulate args handling
            }
        }
    }

    /*
        perform a search-and-replace in the value of an mencoder option
        TODO: signature: handle null, a single map and an array of maps
    */
    // DSL method
    void replace(Map<String, Map<String, String>> replace_map) {
        subactions << { stash, args, state ->
            // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
            replace_map.each { name, map ->
                name = "-$name"
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
        subactions << { stash, args, state ->
            def uri = stash['uri']
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
                        let(stash, 'uri', stream_uri)
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
}
