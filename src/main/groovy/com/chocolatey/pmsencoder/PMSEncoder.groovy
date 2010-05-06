@Typed
package com.chocolatey.pmsencoder

interface ActionClosure {
    void call(Stash stash, List<String> args)
}

interface MatchClosure {
    boolean call(Stash stash, List<String> args)
}

class Stash extends LinkedHashMap<String, String> {
    Stash() {
        super()
    }

    Stash(Stash old) {
        super(old)
        // old.each { name, value -> put(name, value) }
        // super(old)
    }

    Stash(Map<Object, Object> old) {
        super()
        old.each { name, value -> put(name.toString(), value.toString()) }
    }
}

class Logger {
    void debug(GString msg) {
        debug(msg.toString())
    }

    void debug(String msg) { }

    void fatal(GString msg) {
        fatal(msg.toString())
    }

    void fatal(String msg) { }
}

class NullLogger extends Logger { }

class Matcher {
    String path
    Config config

    Matcher(String path, Logger logger = new NullLogger()) {
        config = new Config(logger)

        if (path != null) {
            logger.debug("loading config: $path")
            this.path = path
            this.load(path)
        }
    }

    private void load(String path) {
        Script script = new GroovyShell().parse(new File(path))
        ExpandoMetaClass emc = new ExpandoMetaClass(script.class, false)

        emc.setProperty('config', config.&config) // set the DSL's sole top-level method: config
        emc.initialize()
        script.metaClass = emc
        script.run()
    }

    List<String> match(Stash stash, List<String> args) {
        config.match(stash, args) // we could use the @Delegate annotation, but this is cleaner/clearer
    }
}

class Config {
    Double version
    private List<Profile> profiles = []
    private Logger logger

    Config(Logger logger) {
        this.logger = logger
    }

    List<String> match(Stash stash, List<String> args) {
        // work around Groovy++'s inner-class-style restriction that outer value types must be final
        List<String> matched = []
        logger.debug("matching URI: ${stash['uri']}")
        profiles.each { profile ->
            Closure actions

            if ((actions = profile.match(stash, args))) {
                actions()
                matched << profile.name
            }
        }

        return matched
    }

    // DSL method
    void version(Double version) {
        this.version = version
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // Groovy++ doesn't support delegation yet
    void config(Closure closure) {
        this.with(closure)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // Groovy++ doesn't support delegation yet
    void profile (String name, Closure closure) {
        Profile profile = new Profile(name, logger)
        profile.with(closure)
        profiles << profile
    }
}

class Profile {
    private Match match
    private Actions actions
    private Logger logger
    public String name

    Profile(String name, Logger logger) {
        this.name = name
        this.logger = logger
    }

    Closure match(Stash stash, List<String> args) {
        // make sure this uses LinkedHashMap (the Groovy default Map implementation)
        // to ensure predictable iteration ordering (for tests)
        Stash new_stash = new Stash(stash) // clone() doesn't work with Groovy++

        if (match.match(new_stash, args)) {
            return {
                logger.debug("matched $name")
                // merge all the name/value bindings resulting from the match
                new_stash.each { name, value -> actions.let(stash, name, value) }
                actions.executeActions(stash, args)
                return name
            }
        } else {
            return null
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // Groovy++ doesn't support delegation yet
    void match(Closure closure) {
        Match match = new Match()
        match.with(closure)
        this.match = match
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // Groovy++ doesn't support delegation yet
    void action (Closure closure) {
        Actions actions = new Actions(logger)
        actions.with(closure)
        this.actions = actions
    }
}

// TODO: add isGreaterThan (gt?), isLessThan (lt?), and equals (eq?) matchers
class Match {
    private List<MatchClosure> matchers = []

    // DSL method
    void matches(Map<String, String> map) {
        map.each { name, value -> matches(name, value) }
    }

    // DSL method
    void matches(String name, String value) {
        matchers << { stash, args ->
            RegexHelper.match(stash[name], value, stash)
        }
    }

    boolean match(Stash stash, List<String> args) {
        /*
            XXX groovy++ can't infer the matcher type here, but
            can infer the action type below; note: it compiles
            fine if every is replaced by each, so it must be per-method
        */
        matchers.every { MatchClosure matcher -> matcher(stash, args) }
    }
}

class Actions {
    /* TODO: add configurable proxy support */
    private List<ActionClosure> actions = []
    @Lazy private HTTPClient http = new HTTPClient()
    private Map<String, String> cache = [:]
    private Logger logger

    Actions(Logger logger) {
        this.logger = logger
        // this.logger = profile.getLogger() // XXX wtf? profile is null
    }

    String executeActions(Stash stash, List<String> args) {
        actions.each { action -> action(stash, args) }
    }

    // not a DSL method: do the heavy-lifting of stash assignment
    // public because Profile needs to call it
    void let(Stash stash, String name, String value) {
        if ((stash[name] == null) || (stash[name] != value)) {
            String[] new_value = [ value ] // FIXME can't get reference to work transparently
            logger.debug("setting \$$name to $value")
         
            stash.each { stash_key, stash_value ->
                /*
                    TODO see if there's a way to do this natively i.e. interpret the string
                    as a GString (with bindings in "stash") rather than performing the
                    interpolation manually. this would also allow the string to contain
                    arbitrary groovy expressions e.g.

                        match {
                            matches uri: '^http://www\\.example\\.com/(?<id>\\d+)\\.html'
                        }

                        action {
                            let uri: 'http://www.example.com/${id + 1}.html'
                        }
                */
                def var_name_regex = ~/(?:(?:\$$stash_key\b)|(?:\$\{$stash_key\}))/
         
                if (new_value[0] =~ var_name_regex) {
                    logger.debug("replacing \$$stash_key with '${stash_value.replaceAll("'", "\\'")}' in ${new_value[0]}")
                    // XXX squashed bug: strings are immutable!
                    new_value[0] = new_value[0].replaceAll(var_name_regex, stash_value)
                }
            }

            // if (new_value[0] != value) {
                stash[name] = new_value[0]
                logger.debug("set \$$name to ${new_value[0]}")
            // }
        }
    }

    /*
        1) get the URI pointed to by stash[uri] (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update stash with any named captures
    */
    // DSL method
    void get(String regex) {
        actions << { stash, args ->
            String uri = stash['uri']
            String document = cache[uri]
            Stash new_stash = new Stash()

            if (!document) {
                document = cache[uri] = http.get(uri)
            }

            assert document

            logger.debug("matching content of $uri against $regex")

            if (RegexHelper.match(document, regex, new_stash)) {
                logger.debug("success")
                new_stash.each { name, value -> let(stash, name, value) }
            } else {
                logger.debug("failure")
            }
        }
    }

    // define a variable in the stash, performing any variable substitutions
    // DSL method
    void let(Map<String, String> map) {
        map.each { key, value ->
            actions << { stash, args -> let(stash, key, value) }
        }
    }

    // set an mencoder option - create it if it doesn't exist
    // DSL method
    void set(Map<String, String> map) {
        actions << { stash, args ->
            // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
            map.each { name, value ->
                logger.debug("inside set: $name => $value")
                name = "-$name"
                int index = args.findIndexOf { it == name }
             
                if (index == -1) {
                    if (value) {
                        logger.debug("adding $name $value")
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
                        logger.debug("adding $name")
                        args << name // FIXME: encapsulate @args handling
                    }
                } else if (value) {
                    logger.debug("setting $name to $value")
                    args[ index + 1 ] = value // FIXME: encapsulate @args handling
                }
            }
        }
    }

    /*
        perform a search-and-replace in the value of an mencoder option
        TODO: signature: handle null, a single map and an array of maps
    */
    // DSL method
    void replace(Map<String, Map<String, String>> replace_map) {
        actions << { stash, args ->
            // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
            replace_map.each { name, map ->
                name = "-$name"
                int index = args.findIndexOf { it == name }
             
                if (index != -1) {
                    map.sort().each { search, replace -> // sort to ensure determinism in the tests
                        logger.debug("replacing $search with $replace")
                        // TODO support named captures
                        // FIXME: encapsulate args handling
                        String value = args[ index + 1 ]

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
        given (in the stash) the $video_id and $t values of a YouTube media URI (i.e. the URI of an .flv, .mp4 &c.),
        construct the full URI with various $fmt values in succession and set the stash $uri value to the first one
        that's valid (based on a HEAD request)

        see http://stackoverflow.com/questions/1883737/getting-an-flv-from-youtube-in-net
    */

    // DSL method
    void youtube(String[] formats) {
        actions << { stash, args ->
            String uri = stash['uri']
            String video_id = stash['video_id']
            String t = stash['t']
            assert video_id
            assert t
            boolean found = false
         
            /*
                via http://www.longtailvideo.com/support/forum/General-Chat/16851/Youtube-blocked-http-youtube-com-get-video

                No &fmt = FLV (very low)
                &fmt=5 = FLV (very low)
                &fmt=6 = FLV (doesn't always work)
                &fmt=13 = 3GP (mobile phone)
                &fmt=18 = MP4 (normal)
                &fmt=22 = MP4 (hd)

                see also:

                http://tinyurl.com/y8rdcoy
                http://userscripts.org/topics/18274
            */
         
            if (formats.size() > 0) {
                found = formats.any { fmt ->
                    String media_uri = "http://www.youtube.com/get_video?fmt=$fmt&video_id=$video_id&t=$t"

                    logger.debug("trying $media_uri")

                    if (http.head(media_uri)) {
                        logger.debug("success")
                        // set the new URI - note use the low-level interface NOT the (deferred) DSL interface!
                        let(stash, 'uri', media_uri)
                        return true
                    } else {
                        logger.debug("failure")
                        return false
                    }
                }
            } else {
                logger.fatal("no formats defined for $uri")
            }
         
            if (!found) {
                logger.fatal("can't retrieve YouTube video from $uri")
            }
        }
    }
}
