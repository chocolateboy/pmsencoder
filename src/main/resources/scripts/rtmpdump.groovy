/*
    pmsencoder://rtmpdump?-v&-r=http%3A//example.com&-y=yvalue&-W=Wvalue

    -o is set automatically
    -r or --rtmp is required
    boolean values can be set without a value e.g. pmsencoder://rtmpdump?--rtmp=http%3A//example.com&--live&--foo=bar
    values *must* be URL-encoded
    keys can be, but hyphens are not special characters, so they don't need to be
*/

init {
    profile ('pmsencoder://rtmpdump') {
        pattern {
            match uri: '^pmsencoder://rtmpdump\\?'
            match { RTMPDUMP }
        }

        action {
            def rtmpdumpArgs = []
            def params = http.getNameValuePairs(uri) // uses URLDecoder.decode to decode the name and value
            def seenURL = false

            for (param in params) {
                def name = param.name
                def value = param.value

                switch (name) {
                    // Special case: don't quote this (JSON) value on Windows
                    case '-j':
                    case '--jtv':
                        rtmpdumpArgs << name << value
                        break
                    case '-r':
                    case '--rtmp':
                        if (value) {
                            uri = quoteURI(value)
                            seenURL = true
                        }
                        break
                    case '-o':
                    case '--flv':
                        break // ignore
                    default:
                        rtmpdumpArgs << name
                        // not all values are URIs, but quoteURI() is harmless (except for -j) on Windows and a no-op on other platforms
                        if (value)
                            rtmpdumpArgs << quoteURI(value)
                }
            }

            if (seenURL) {
                // rtmpdump doesn't log to stdout, so no need to use -q on Windows
                downloader = "$RTMPDUMP -o $DOWNLOADER_OUT -r ${uri}"
                downloader += rtmpdumpArgs
            } else {
                logger.error("invalid rtmpdump:// URI: no -r or --rtmp parameter supplied: ${uri}")
            }
        }
    }
}
