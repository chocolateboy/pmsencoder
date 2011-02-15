script {
    profile ('HTTP Live Stream') {
        pattern {
            match $URI: '\\.m3u8$'
            match { HLS_PLAYER != null }
        }

        action {
            $DOWNLOADER = "$PYTHON $HLS_PLAYER --path $DOWNLOADER_OUT \"${$URI}\""
        }
    }
}
