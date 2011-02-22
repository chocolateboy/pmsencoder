/*
    rtmpdump://channel?url=http%3A//example.com&-y=yvalue&-c=cvalue

    -q, -o and -r are set automatically (the latter via the url parameter)
    boolean values can be set without a value e.g. rtmpdump://channel?url=http%3A//example.com&--live&--foo=bar
    values *must* be URL-encoded
    keys can be, but hyphens are not special characters, so they don't need to be
*/

init {
    profile ('rtmpdump://') {
        pattern {
            protocol 'rtmpdump'
            match { RTMPDUMP }
        }

        action {
            def rtmpdumpArgs = []
            def pairs = URLEncodedUtils.parse($URI)

            for (pair in pairs) {
                def name = URLDecoder.decode(pair.name)
                def value = URLDecoder.decode(pair.value)
                def seenURL = false

                switch (name) {
                    case 'url':
                        $URI = quoteURI(value)
                        seenURL = true
                        break
                    default:
                        rtmpdumpArgs << name
                        if (value) {
                            rtmpdumpArgs << value
                        }
                }

                if (seenURL) {
                    $DOWNLOADER = "$RTMPDUMP -q -o $DOWNLOADER_OUT -r ${$URI}"
                    $DOWNLOADER += rtmpdumpArgs
                } else {
                    log.error("invalid rtmpdump:// URI: no url parameter supplied: ${$URI}")
                }
            }
        }
    }
}
