script {
    profile ('Apple Trailers') {
        pattern {
            match uri: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        action {
            if (FFMPEG_HTTP_HEADERS) {
                set '-headers': 'User-Agent: QuickTime/7.6.2'
            } else {
                transcoder = MENCODER
                set '-user-agent': 'QuickTime/7.6.2'
            }
        }
    }
}
