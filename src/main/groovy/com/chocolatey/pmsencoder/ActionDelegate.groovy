package com.chocolatey.pmsencoder

import org.apache.http.NameValuePair

// FIXME investigate why this doesn't work. typed closures?
// @groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
class ActionDelegate {
    private Closure<List<String>> contextThunk
    @Delegate final ProfileDelegate profileDelegate
    // FIXME: sigh: transitive delegation doesn't work (Groovy bug)
    @Delegate private final Matcher matcher

    ActionDelegate(ProfileDelegate profileDelegate) {
        this.profileDelegate = profileDelegate
        this.matcher = profileDelegate.matcher
        setContextThunk(this.&getTranscoder) // default context
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
     * note: we use a thunk e.g.:
     *
     *     { return self.transcoder }
     *
     * rather than a list e.g.:
     *
     *     self.transcoder
     *
     * because the targeted list (transcoder, hook &c.)
     * is mutable e.g. we need to modify self.transcoder, rather than a reference
     * to transcoder, which may no longer be used
     *
     * FIXME (Groovy++) Every other attempt to get this simple thunk to work
     * fails (including the recommended Function0<Closure>>).
     *
     * TODO try to clean this up now we're using Groovy 2
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
    //     Duplicate method name&signature in class file com/chocolatey/pmsencoder/ActionDelegate$hook$2
    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    void hook (Closure closure) {
        if (getHook() == null) {
            logger.error("can't modify null hook command list")
        } else {
            def oldContextThunk = getContextThunk() // support nested blocks
            setContextThunk(this.&getHook)

            try {
                closure.call()
            } finally {
                setContextThunk(oldContextThunk)
            }
        }
    }

    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    void downloader (Closure closure) {
        if (getDownloader() == null) {
            logger.error("can't modify null downloader command list")
        } else {
            def oldContextThunk = getContextThunk() // support nested blocks
            setContextThunk(this.&getDownloader)
            try {
                closure.call()
            } finally {
                setContextThunk(oldContextThunk)
            }
        }
    }

    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    void transcoder (Closure closure) {
        if (getTranscoder() == null) {
            logger.error("can't modify null transcoder command list")
        } else {
            def oldContextThunk = getContextThunk() // support nested blocks
            setContextThunk(this.&getTranscoder)

            try {
                closure.call()
            } finally {
                setContextThunk(oldContextThunk)
            }
        }
    }

    // DSL method
    boolean setAudioBitrateOptions(Object maybeList) {
        return maybeList == null ? null : command.setAudioBitrateOptions(Util.toStringList(maybeList, true))
    }

    // DSL method
    boolean setVideoBitrateOptions(Object maybeList) {
        return maybeList == null ? null : command.setVideoBitrateOptions(Util.toStringList(maybeList, true))
    }

    // DSL method
    boolean setVideoTranscodeOptions(Object maybeList) {
        return maybeList == null ? null : command.setVideoTranscodeOptions(Util.toStringList(maybeList, true))
    }

    // DSL accessor (ffmpegPath): getter
    String getFfmpegPath() {
        return command.getFfmpegPath()
    }

    // DSL method
    String shellQuote(Object uri) {
        return Util.shellQuote(uri)
    }

    // DSL method
    void set(Map map) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        map.each { name, value -> setOption(name.toString(), value?.toString()) }
    }

    // DSL method
    void set(Object name) {
        setOption(name.toString(), null)
    }

    // set an option in the current command list (context) - create the option if it doesn't exist
    private void setOption(String name, String value = null) {
        assert name != null

        def context = getContext()
        def index = context.findIndexOf { it == name }

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

        if (context.size() <= 1) {
            context << object.toString()
        } else {
            context.add(1, object.toString())
        }

        return context
    }

    List<String> prepend(List list) {
        // XXX we need to be careful to modify the list in place
        def context = getContext()

        if (context.size() <= 1) {
            context.addAll(list*.toString())
        } else {
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
        perform a string search-and-replace in the value of an option.
    */

    // DSL method
    void replace(Map<Object, Map> replaceMap) {
        def context = getContext()
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        replaceMap.each { name, map ->
            // squashed bug (see above): take care to modify context in-place
            int index = context.findIndexOf { String it -> it == name.toString() }

            if (index != -1) {
                map.each { Object search, Object replace ->
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

    // take a query string (a list of key=value pairs joined by '&') and
    // return them as a map. if a key is seen more than once, overwrite the value.
    // XXX note: URLEncodedUtils.parse (which getNameValueMap calls) unescapes the key and value
    // XXX numerous type-inference fails, hence the explicit types
    private Map<String, String> queryStringToMap(String qs) {
        /*
            XXX squashed bug: don't try to implement this by hand (with tokenize()) - there
            are too many gotchas e.g. "pairs" (split on '=') with 1 or 3 elements as well
            as the expected 2...
        */
        return getHttp().getNameValueMap(qs)
    }

    // XXX numerous type-inference fails, hence the explicit types
    private Map<String, String> getFmtURLMap(String video_id) {
        // the initial values of variables don't appear to be documented for Groovy, so assume the same as Java
        // i.e. explicitly initialize
        String token = null
        Map<String, String> videoInfoMap
        Map<String, String> fmtURLMap = null
        List<String> elements = [ '&el=embedded', '&el=detailpage', '&el=vevo' , '' ]

        for (String el : elements) {
            def uri = "http://www.youtube.com/get_video_info?video_id=${video_id}${el}&ps=default&eurl=&gl=US&hl=en"
            String document = getHttp().get(uri)

            if (document == null) {
                logger.warn("Can't download metadata for $video_id from $uri")
            } else {
                /*
                    the get_video_info file consists of key/value pairs in query-string format e.g.: key1=value1&key2=value2 &c.
                    we want the token and url_encoded_fmt_stream_map values, so first we need to convert the file into a map:

                    [
                        token: '...',
                        url_encoded_fmt_stream_map: '...',
                        video_id: _OBlgSz8sSM,
                        author: HDCYT,
                        ...
                    ]
                */

                // queryStringToMap decodes the values (and keys) for us, so no need to unescape
                videoInfoMap = queryStringToMap(document)

                // the token is no longer used, but youtube-dl uses it to sanity check the metadata,
                // so we do the same here
                token = videoInfoMap['token'] // extract the token

                if (token) {
                    break
                } else {
                    String reason = videoInfoMap['reason'] ?: 'unknown error'
                    logger.error("Can't find authentication token for $video_id in $uri: $reason")
                }
            }
        }

        if (token) {
            /*
                next we need to extract the url_encoded_fmt_stream_map value.
                it represents a list of maps, separated by commas:

                    url_encoded_fmt_stream_map=map1,map2,map3

                each map is a URL-encoded string that uses the same query-string format
                as before e.g. key1=value1&key2=value2 &c. each map contains the
                following keys:

                    fallback_host
                    itag
                    url
                    quality
                    sig
                    type

                we only care about:

                    itag: the YouTube fmt number
                    url: the stream URL
                    sig: appended to the stream URL
            */

            // assemble [ fmt, url ] pairs into a map
            fmtURLMap = videoInfoMap['url_encoded_fmt_stream_map'].
                tokenize(',').
                collectEntries { String encodedMap ->
                    // queryStringToMap decodes the values (and keys) for us, so no need to unescape
                    Map<String, String> map = queryStringToMap(encodedMap)
                    String fmt = map['itag']
                    // https://github.com/rg3/youtube-dl/issues/427
                    String url = String.format('%s&signature=%s', map['url'], map['sig'])
                    return [ fmt, url ]
                }
        }

        return fmtURLMap
    }

    // DSL method
    void youtube(List<Integer> formats = getYOUTUBE_ACCEPT()) {
        def command = getCommand()
        def uri = command.getVarAsString('uri')
        def video_id = command.getVarAsString('youtube_video_id')
        def found = false

        assert video_id != null

        command.setVar('youtube_uri', uri)

        if (formats.size() > 0) {
            def fmtURLMap = getFmtURLMap(video_id)
            if (fmtURLMap != null) {
                logger.trace('fmt -> URL map: ' + fmtURLMap)

                found = formats.any { fmt ->
                    def fmtString = fmt.toString()
                    logger.debug("checking fmt -> URL map for $fmtString")
                    def streamURI = fmtURLMap[fmtString]

                    if (streamURI != null) {
                        // set the new URI
                        logger.debug('success')
                        command.setVar('youtube_fmt', fmtString)
                        command.setVar('uri', streamURI)
                        return true // i.e. found = true
                    } else {
                        logger.debug('failure')
                        return false // i.e. found = false
                    }
                }
            } else {
                logger.fatal("can't find fmt -> URL map in video metadata")
            }
        }  else {
            logger.fatal("no formats defined for $uri")
        }

        if (!found) {
            logger.fatal("can't retrieve stream URL for $uri")
        }
    }
}
