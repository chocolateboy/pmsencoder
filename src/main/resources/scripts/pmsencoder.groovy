init {
    profile ('pmsencoder://') {
        pattern {
            protocol 'pmsencoder'
        }

        // currently three values can be set
        // the URI (required)
        // the referrer (optional)
        // the user-agent (optional)
        action {
            def pairs = URLEncodedUtils.parse($URI)
            def setDownloaderArg = { key, value ->
                if (!$DOWNLOADER) {
                    $DOWNLOADER = $MPLAYER + [ key, value ]
                } else if ($DOWNLOADER.size() > 0 && $DOWNLOADER[0] == 'MPLAYER') {
                    downloader {
                        set([ (key): value ]) // (key) - don't treat it as 'key'
                    }
                }
            }

            for (pair in pairs) {
                def name = pair.name
                def value = pair.value
                def seenURI = false

                switch (name) {
                    case 'uri':
                        $URI = URLDecoder.decode(value)
                        seenURI = true
                        break
                    case 'referrer':
                        // this requires a recent (post June 2010) MPlayer
                        setDownloaderArg('-referrer', URLDecoder.decode(value))
                        break
                    case 'user_agent':
                        setDownloaderArg('-user-agent', URLDecoder.decode(value))
                        break
                }

                if (!seenURI) {
                    log.error("invalid pmsencoder:// URI: no uri parameter supplied: ${$URI}")
                }
            }
        }
    }
}
