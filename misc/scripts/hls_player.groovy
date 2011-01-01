script {
    def PYTHON = '/path/to/python'
    def HLS_PLAYER = '/path/to/hls-player'

    profile ('HTTP Live Stream') {
        pattern {
            match $URI: '\\.m3u8$'
        }

        action {
            $DOWNLOADER = "$PYTHON $HLS_PLAYER --path $DOWNLOADER_OUT ${$URI}"
        }
    }
}
