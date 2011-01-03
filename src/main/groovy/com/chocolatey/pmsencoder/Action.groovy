@Typed
package com.chocolatey.pmsencoder

class Action extends CommandDelegate {
    Action(Script script, Command command) {
        super(script, command)
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

    /*
        1) get the URI pointed to by options['uri'] or stash.get('$URI') (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update the stash with any named captures
    */

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

    // set a transcoder option - create it if it doesn't exist
    void setArg(Object name, Object value = null) {
        assert name != null

        def index = $ARGS.findIndexOf { it == name.toString() }

        if (index == -1) {
            if (value != null) {
                log.debug("adding $name $value")
                /*
                    XXX squashed bug: careful not to perform operations on copies of stash or args
                    i.e. make sure they're modified in place:

                        def args = command.args
                        args += ... // XXX doesn't modify command.args
                */
                $ARGS << name.toString() << value.toString()
            } else {
                log.debug("adding $name")
                $ARGS << name.toString()
            }
        } else if (value != null) {
            log.debug("setting $name to $value")
            $ARGS[ index + 1 ] = value.toString()
        }
    }

    /*
        perform a string search-and-replace in the value of a transcoder option.
    */

    // DSL method
    void replace(Map<Object, Map> replaceMap) {
        // the sort order is predictable (for tests) as long as we (and Groovy) use LinkedHashMap
        replaceMap.each { name, map ->
            // squashed bug (see  above): take care to modify $ARGS in-place
            def index = $ARGS.findIndexOf { it == name.toString() }

            if (index != -1) {
                map.each { search, replace ->
                    if ((index + 1) < $ARGS.size()) {
                        // TODO support named captures
                        log.debug("replacing $search with $replace in $name")
                        def value = $ARGS[ index + 1 ]
                        // XXX bugfix: strings are immutable!
                        $ARGS[ index + 1 ] = value.replaceAll(search.toString(), replace.toString())
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
            def regex = '\\bfmt_url_map=(?<youtube_fmt_url_map>[^&]+)'
            def newStash = new Stash()
            def document = http.get(uri)

            if ((document != null) && RegexHelper.match(document, regex, newStash)) {
                // XXX type-inference fail
                List<String> fmt_url_pairs = URLDecoder.decode(newStash.get('$youtube_fmt_url_map')).tokenize(',')
                fmt_url_pairs.inject(fmt_url_map) { Map<String, String> map, String pair ->
                    // XXX type-inference fail
                    List<String> kv = pair.tokenize('|')
                    map[kv[0]] = kv[1]
                    return map
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
        def stash = command.stash
        def uri = stash.get('$URI')
        def video_id = stash.get('$youtube_video_id')
        def t = stash.get('$youtube_t')
        def found = false

        assert video_id != null
        assert t != null

        command.let('$youtube_uri', uri)

        if (formats.size() > 0) {
            def fmt_url_map = getFormatURLMap(video_id)
            if (fmt_url_map != null) {
                log.trace('fmt_url_map: ' + fmt_url_map)

                found = formats.any { fmt ->
                    log.debug("checking fmt_url_map for $fmt")
                    def stream_uri = fmt_url_map[fmt.toString()]

                    if (stream_uri != null) {
                        // set the new URI
                        log.debug('success')
                        command.let('$youtube_fmt', fmt)
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

    // DSL method: append a list of options to the command's args list
    void append(String object) {
        command.args = command.args + [ object.toString() ]
    }

    void append(List list) {
        list.each { append(it.toString()) }
    }

    // DSL method: prepend a list of options to the command's args list
    void prepend(Object object) {
        command.args = [ object.toString() ] + command.args
    }

    void prepend(List list) {
        list.each { prepend(it.toString()) }
    }

    // DSL method: remove multiple option names and their corresponding values if they have one
    void remove(List optionNames) {
        optionNames.each { remove(it) }
    }

    // DSL method: remove a single option name and its corresponding value if it has one
    void remove(Object optionName) {
        assert optionName != null

        def index = $ARGS.findIndexOf { it == optionName.toString() }

        if (index >= 0) {
            def lastIndex = $ARGS.size() - 1
            def nextIndex = index + 1

            if (nextIndex <= lastIndex) {
                def nextArg = $ARGS[ nextIndex ]

                if (isOption(nextArg)) {
                    log.debug("removing: $optionName")
                } else {
                    log.debug("removing: $optionName $nextArg")
                    $ARGS.remove(nextIndex) // remove this first so the index is preserved for the option name
                }
            }

            $ARGS.remove(index)
        }
    }
}
