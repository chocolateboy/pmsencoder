@Typed
package com.chocolatey.pmsencoder

class Action {
    private Closure _context
    @Delegate final ProfileDelegate profileDelegate
    // FIXME: sigh: transitive delegation doesn't work (groovy bug)
    @Delegate private final Matcher matcher

    Action(ProfileDelegate profileDelegate) {
        this.profileDelegate = profileDelegate
        this.matcher = profileDelegate.matcher
        setContext({ get$TRANSCODER() })
    }

    boolean isOption(String arg) {
        // a rare use case for Groovy's annoyingly lax definition of false.
        // and it's not really a use case because it requires three lines of
        // explanation: null and an empty string evaluate as false

        if (arg) {
           return arg ==~ /^-[^0-9].*$/
        } else {
            return false
        }
    }

    // FIXME Every other attempt to get this simple thunk to work
    // fails (including the recommended Function0<List<String>>)
    void setContext(Closure closure) {
        _context = closure
    }

    List<String> getContext() {
        _context.call()
    }

    // FIXME: weird compiler errors complaining about methods with the same name/signature
    // in Groovy++ without this:
    //
    //     Duplicate method name&signature in class file com/chocolatey/pmsencoder/Action$hook$2
    @Typed(TypePolicy.DYNAMIC)
    void hook (Closure closure) {
        if ($HOOK == null) {
            log.error("can't modify null hook command list")
        } else {
            setContext({ get$HOOK() })
            try {
                closure.call()
            } finally {
                setContext({ get$TRANSCODER() })
            }
        }
    }

    @Typed(TypePolicy.DYNAMIC)
    void downloader (Closure closure) {
        if ($DOWNLOADER == null) {
            log.error("can't modify null downloader command list")
        } else {
            setContext({ get$DOWNLOADER() })
            try {
                closure.call()
            } finally {
                setContext({ get$TRANSCODER() })
            }
        }
    }

    @Typed(TypePolicy.DYNAMIC)
    void transcoder (Closure closure) {
        if ($TRANSCODER == null) {
            log.error("can't modify null transcoder command list")
        } else {
            assert getContext().is(get$TRANSCODER())
            try {
                closure.call()
            } finally {
                setContext({ get$TRANSCODER() }) // in case of nested blocks
            }
        }
    }

    @Typed(TypePolicy.DYNAMIC)
    void output (Closure closure) {
        if ($OUTPUT == null) {
            log.error("can't modify null ffmpeg output arg list")
        } else {
            setContext({ get$OUTPUT() })
            try {
                closure.call()
            } finally {
                setContext({ get$TRANSCODER() }) // in case of nested blocks
            }
        }
    }

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

    // set an option in the current argument list (context) - create the option if it doesn't exist
    void setArg(Object name, Object value = null) {
        assert name != null

        def context = getContext()
        def index = context.findIndexOf { it == name.toString() }

        if (index == -1) {
            if (value != null) {
                log.debug("adding $name $value")
                /*
                    XXX squashed bug: careful not to perform operations on copies of stash or transcoder
                    i.e. make sure they're modified in place:

                        def transcoder = command.transcoder
                        transcoder += ... // XXX doesn't modify command.transcoder
                */
                context << name.toString() << value.toString()
            } else {
                log.debug("adding $name")
                context << name.toString()
            }
        } else if (value != null) {
            log.debug("setting $name to $value")
            context[ index + 1 ] = value.toString()
        }
    }

    // DSL method: append a single option to the current argument list
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

    // DSL method: prepend a single option to the current argument list
    List<String> prepend(Object object) {
        // XXX we need to be careful to modify the list in place as context
        // is just a reference to list rather than a field in its own
        // right

        def context = getContext()
        def contextSize = context.size()

        if (contextSize == 0) {
            context << object.toString()
        } else {
            if (context.is(get$OUTPUT())) { // no executable in the first element to protect
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
        } else if (context.is(get$OUTPUT())) { // no executable in the first element to protect
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
                    log.debug("removing: $optionName")
                } else {
                    log.debug("removing: $optionName $nextArg")
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
                        log.debug("replacing $search with $replace in $name")
                        def value = context[ index + 1 ]
                        // XXX squashed bug: strings are immutable!
                        context[ index + 1 ] = value.replaceAll(search.toString(), replace.toString())
                    } else {
                        log.warn("can't replace $search with $replace in $name: target out of bounds")
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
            def document = $HTTP.get(uri)

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
                def url_data_strs = URLDecoder.decode(newStash.get('$youtube_fmt_url_map')).tokenize(',')
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
    void youtube(List<Integer> formats = $YOUTUBE_ACCEPT) {
        def uri = command.getVar('$URI')
        def video_id = command.getVar('$youtube_video_id')
        def t = command.getVar('$youtube_t')
        def found = false

        assert video_id != null
        assert t != null

        command.let('$youtube_uri', uri)

        if (formats.size() > 0) {
            def fmt_url_map = getFormatURLMap(video_id)
            if (fmt_url_map != null) {
                log.trace('fmt_url_map: ' + fmt_url_map)

                found = formats.any { fmt ->
                    def fmtString = fmt.toString()
                    log.debug("checking fmt_url_map for $fmtString")
                    def stream_uri = fmt_url_map[fmtString]

                    if (stream_uri != null) {
                        // set the new URI
                        log.debug('success')
                        command.let('$youtube_fmt', fmtString)
                        command.let('$URI', stream_uri)
                        return true
                    } else {
                        log.debug('failure')
                        return false
                    }
                }
            } else {
                log.fatal("can't find fmt -> URI map in video metadata")
            }
        }  else {
            log.fatal("no formats defined for $uri")
        }

        if (!found) {
            log.fatal("can't retrieve stream URI for $uri")
        }
    }
}
