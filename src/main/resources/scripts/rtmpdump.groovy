// e.g. rtmpdump://channel?uri=http%3A//example.com&-y=yvalue&-c=cvalue
// -q, -o and -r are set automatically
// boolean values can be set without a value e.g. rtmpdump://channel?uri=http%3A//example.com&--live&--foo=bar

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
                def seenURI = false

                switch (name) {
                    case 'uri':
                        $URI = quoteURI(value)
                        seenURI = true
                        break
                    default:
                        rtmpdumpArgs << name
                        if (value != null) {
                            rtmpdumpArgs << value
                        }
                }

                if (seenURI) {
                    $DOWNLOADER = "$RTMPDUMP -q -o $DOWNLOADER_OUT -r ${$URI}"
                    $DOWNLOADER += rtmpdumpArgs
                } else {
                    log.error("invalid rtmpdump:// URI: no uri parameter supplied: ${$URI}")
                }
            }
        }
    }
}
