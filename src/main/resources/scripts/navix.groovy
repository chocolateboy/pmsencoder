/*
    navix://channel?url=http%3A//example.com&referer=http%3A//example.com&agent=Mozilla

    This protocol uses MPlayer as the downloader
    Only the following Navi-X output fields are supported:

        url     // required: media URL - note: this *can't* be an rtmpdump:// URL
        agent   // optional: HTTP user-agent
        referer // optional: HTTP referrer
        player  // optional: currently ignored

    Although most fields are optional, there is no point using this protocol unless
    at least one optional field is supplied.

    boolean values (none currently) can be set without a value e.g. navix://channel?url=http%3A//example.com&foo
    values *must* be URL-encoded
    keys are just alphanumeric, so don't need to be
*/

init {
    profile ('navix://') {
        pattern {
            protocol 'navix'
        }

        action {
            def mplayerArgs = []
            def pairs = $HTTP.getNameValuePairs($URI)
            def seenURL = false

            for (pair in pairs) {
                def name = URLDecoder.decode(pair.name)
                def value = URLDecoder.decode(pair.value)

                switch (name) {
                    case 'url':
                        if (value) {
                            // quote handling is built in for this downloader
                            $URI = value
                            seenURL = true
                        }
                        break
                    case 'referer':
                        if (value)
                            mplayerArgs << '-referrer' << value // requires a recent (>= June 2010) mplayer
                        break
                    case 'agent':
                        if (value)
                            mplayerArgs << '-user-agent' << value
                        break
                    case 'player':
                        if (value)
                            log.info("player option for navix:// protocol currently ignored: ${value}")
                        break
                    default:
                        log.warn("unsupported navix:// option: ${name}=${value}")
                }
            }

            if (seenURL) {
                $DOWNLOADER = $MPLAYER + mplayerArgs
            } else {
                log.error("invalid navix:// URI: no url parameter supplied: ${$URI}")
            }
        }
    }
}
