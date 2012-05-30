script {
    profile ('Apple Trailers') {
        pattern {
            match $URI: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        action {
            if (FFMPEG_HTTP_HEADERS) {
                set '-headers': 'User-Agent: QuickTime/7.6.2'
            } else {
                $TRANSCODER = $MENCODER
                set '-user-agent': 'QuickTime/7.6.2'
            }
        }
    }
}
