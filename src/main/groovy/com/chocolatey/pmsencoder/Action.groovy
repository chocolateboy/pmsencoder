@Typed
package com.chocolatey.pmsencoder

class Action {
    private Closure contextThunk
    @Delegate final ProfileDelegate profileDelegate
    // FIXME: sigh: transitive delegation doesn't work (Groovy bug)
    @Delegate private final Matcher matcher

    Action(ProfileDelegate profileDelegate) {
        this.profileDelegate = profileDelegate
        this.matcher = profileDelegate.matcher
        setContextThunk({ getTranscoder() }) // default context
    }

    boolean isOption(String arg) {
        // a rare use case for Groovy's annoyingly lax definition of false.
        // and it's not really a use case because it requires three lines of
        // explanation: null and an empty string evaluate as false

        if (arg) {
           return arg ==~ /^--?[a-zA-Z][a-zA-Z_-]*$/
        } else {
            return false
        }
    }

    /*
     * PMSEncoder supports multiple command lists e.g. hook commands,
     * downloader commands. These can all be manipulated by the methods
     * in this class. The target command list, called the context, is
     * set by using a context block. The default context is the transcoder.
     *
     *     set '-foo', 'bar' // (implicit) transcoder context
     *
     *     transcoder { // (explicit) transcoder context
     *         set '-foobar'
     *     }
     *
     *     hook {
     *         append '-foo'
     *         remove '-bar'
     *     }
     *
     *     set '-baz', 'quux' // back to transcoder
     *
     * note: we use a thunk because the targeted list (transcoder, hook &c.)
     * is mutable e.g. we need to modify self.transcoder, rather than a reference
     * to transcoder, which may no longer be used
     *
     * FIXME Every other attempt to get this simple thunk to work
     * fails (including the recommended Function0<Closure>>).
     */
    private void setContextThunk(Closure closure) {
        contextThunk = closure
    }

    private Closure getContextThunk() {
        assert contextThunk
        return contextThunk
    }

    private List<String> getContext() {
        assert contextThunk
        contextThunk.call()
    }

    // FIXME: weird compiler errors complaining about methods with the same name/signature
    // in Groovy++ without this:
    //
    //     Duplicate method name&signature in class file com/chocolatey/pmsencoder/Action$hook$2
    @Typed(TypePolicy.DYNAMIC)
    void hook (Closure closure) {
        if (getHook() == null) {
            logger.error("can't modify null hook command list")
        } else {
            def oldContextThunk = getContextThunk() // support nested blocks
            setContextThunk({ getHook() })

            try {
                closure.call()
            } finally {
                setContextThunk(oldContextThunk)
            }
        }
    }

    @Typed(TypePolicy.DYNAMIC)
    void downloader (Closure closure) {
        if (getDownloader() == null) {
            logger.error("can't modify null downloader command list")
        } else {
            def oldContextThunk = getContextThunk() // support nested blocks
            setContextThunk({ getDownloader() })
            try {
                closure.call()
            } finally {
                setContextThunk(oldContextThunk)
            }
        }
    }

    @Typed(TypePolicy.DYNAMIC)
    void transcoder (Closure closure) {
        if (getTranscoder() == null) {
            logger.error("can't modify null transcoder command list")
        } else {
            def oldContextThunk = getContextThunk() // support nested blocks
            setContextThunk({ getTranscoder() })

            try {
                closure.call()
            } finally {
                setContextThunk(oldContextThunk)
            }
        }
    }

    // FIXME this is obsolete - ffmpeg output settings should be governed
    // entirely by the capabilities of the renderer
    @Typed(TypePolicy.DYNAMIC)
    void output (Closure closure) {
        if (getOutput() == null) {
            logger.error("can't modify null ffmpeg output arg list")
        } else {
            def oldContextThunk = getContextThunk() // support nested blocks
            setContextThunk({ getOutput() })
            try {
                closure.call()
            } finally {
                setContextThunk(oldContextThunk)
            }
        }
    }

    // DSL method
    String quoteURI(String uri) {
        Util.quoteURI(uri)
    }

    // define a variable in the stash
    // DSL method
    void let(Map map) {
        map.each { key, value ->
            command.let(key, ((value == null) ? null : value))
        }
    }

    // DSL method
    void set(Map map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> setArg(name, (value == null ? null : value)) }
    }

    // DSL method
    void set(Object name) {
        setArg(name.toString(), null)
    }

    // set an option in the current command list (context) - create the option if it doesn't exist
    void setArg(Object name, Object value = null) {
        assert name != null

        def context = getContext()
        def index = context.findIndexOf { it == name.toString() }

        if (index == -1) {
            if (value != null) {
                logger.debug("adding $name $value")
                /*
                    XXX squashed bug: careful not to perform operations on copies of stash or transcoder
                    i.e. make sure they're modified in place:

                        def transcoder = command.transcoder
                        transcoder += ... // XXX doesn't modify command.transcoder
                */
                context << name.toString() << value.toString()
            } else {
                logger.debug("adding $name")
                context << name.toString()
            }
        } else if (value != null) {
            logger.debug("setting $name to $value")
            context[ index + 1 ] = value.toString()
        }
    }

    // DSL method: append a single option to the current command list
    List<String> append(Object object) {
        def context = getContext()
        context << object.toString()
        return context
    }

    List<String> append(List list) {
        def context = getContext()
        context.addAll(list*.toString())
        return context
    }

    // DSL method: prepend a single option to the current command list
    List<String> prepend(Object object) {
        // XXX we need to be careful to modify the list in place
        def context = getContext()
        def contextSize = context.size()

        if (contextSize == 0) {
            context << object.toString()
        } else {
            if (context.is(getOutput())) { // no executable in the first element to protect
                context.add(0, object.toString())
            } else {
                if (contextSize == 1) {
                    context << object.toString()
                } else {
                    context.add(1, object.toString())
                }
            }
        }

        return context
    }

    List<String> prepend(List list) {
        def context = getContext()

        if (context.isEmpty()) {
            context.addAll(list*.toString())
        } else if (context.is(getOutput())) { // no executable in the first element to protect
            def temp = list*.toString()
            context.addAll(0, list*.toString())
        } else {
            def temp = list*.toString()
            context.addAll(1, list*.toString())
        }

        return context
    }

    // DSL method: remove multiple option names and their corresponding values if they have one
    void remove(List optionNames) {
        optionNames.each { remove(it) }
    }

    // DSL method: remove a single option name and its corresponding value if it has one
    void remove(Object optionName) {
        assert optionName != null

        def context = getContext()
        def index = context.findIndexOf { it == optionName.toString() }

        if (index >= 0) {
            def lastIndex = context.size() - 1
            def nextIndex = index + 1

            if (nextIndex <= lastIndex) {
                def nextArg = context[ nextIndex ]

                if (isOption(nextArg)) {
                    logger.debug("removing: $optionName")
                } else {
                    logger.debug("removing: $optionName $nextArg")
                    context.remove(nextIndex) // remove this first so the index is preserved for the option name
                }
            }

            context.remove(index)
        }
    }

    /*
        perform a string search-and-replace in the value of a transcoder option.
    */

    // DSL method
    void replace(Map<Object, Map> replaceMap) {
        def context = getContext()
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        replaceMap.each { name, map ->
            // squashed bug (see  above): take care to modify context in-place
            def index = context.findIndexOf { it == name.toString() }

            if (index != -1) {
                map.each { search, replace ->
                    if ((index + 1) < context.size()) {
                        // TODO support named captures
                        logger.debug("replacing $search with $replace in $name")
                        def value = context[ index + 1 ]
                        // XXX squashed bug: strings are immutable!
                        context[ index + 1 ] = value.replaceAll(search.toString(), replace.toString())
                    } else {
                        logger.warn("can't replace $search with $replace in $name: target out of bounds")
                    }
                }
            }
        }
    }

    private Map<String, String> getFormatURLMap(String video_id) {
        def fmt_url_map = [:]

        def found = [ '&el=embedded', '&el=detailspage', '&el=vevo' , '' ].any { String param ->
            def uri = "http://www.youtube.com/get_video_info?video_id=${video_id}${param}&ps=default&eurl=&gl=US&hl=en"
            def regex = '\\burl_encoded_fmt_stream_map=(?<youtube_fmt_url_map>[^&]+)'
            def newStash = new Stash()
            def document = getHttp().get(uri)

            if ((document != null) && RegexHelper.match(document, regex, newStash)) {
                /*
                    each url_data_str in url_data_strs is a URL-encoded map containing the following keys:

                        fallback_host
                        itag
                        quality
                        type
                        url

                    we only care about:

                        itag: the YouTube fmt number
                        url: the stream URL
                */

                // XXX numerous type-inference fails
                def url_data_strs = URLDecoder.decode(newStash.get('youtube_fmt_url_map')?.toString()).tokenize(',')
                url_data_strs.inject(fmt_url_map) { Map<String, String> _fmt_url_map, String url_data_str ->
                    // collectEntries (new in Groovy 1.7.9) makes a map out of a list of pairs
                    Map<String, String> url_data_map = url_data_str.tokenize('&').collectEntries { String pair ->
                        pair.tokenize('=')
                    }
                    _fmt_url_map[URLDecoder.decode(url_data_map['itag'])] = URLDecoder.decode(url_data_map['url'])
                    return _fmt_url_map
                }
                return true
            } else {
                return false
            }
        }

        return found ? fmt_url_map : null
    }

    // DSL method
    void youtube(List<Integer> formats = YOUTUBE_ACCEPT) {
        def uri = command.getVar('uri')
        def video_id = command.getVar('youtube_video_id')
        def t = command.getVar('youtube_t')
        def found = false

        assert video_id != null
        assert t != null

        command.let('youtube_uri', uri)

        if (formats.size() > 0) {
            def fmt_url_map = getFormatURLMap(video_id)
            if (fmt_url_map != null) {
                logger.trace('fmt_url_map: ' + fmt_url_map)

                found = formats.any { fmt ->
                    def fmtString = fmt.toString()
                    logger.debug("checking fmt_url_map for $fmtString")
                    def stream_uri = fmt_url_map[fmtString]

                    if (stream_uri != null) {
                        // set the new URI
                        logger.debug('success')
                        command.let('youtube_fmt', fmtString)
                        command.let('uri', stream_uri)
                        return true
                    } else {
                        logger.debug('failure')
                        return false
                    }
                }
            } else {
                logger.fatal("can't find fmt -> URI map in video metadata")
            }
        }  else {
            logger.fatal("no formats defined for $uri")
        }

        if (!found) {
            logger.fatal("can't retrieve stream URI for $uri")
        }
    }
}
