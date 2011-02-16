script {
    profile ('HTTP Live Stream') {
        pattern {
            match { PYTHON && HLS_PLAYER }
            match $URI: '\\.m3u8$'
        }

        action {
            $URI = quoteURI($URI)
            $DOWNLOADER = "$PYTHON $HLS_PLAYER --path $DOWNLOADER_OUT ${$URI}"
        }
    }
}
