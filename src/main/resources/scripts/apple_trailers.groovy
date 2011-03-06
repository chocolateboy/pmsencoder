script {
    profile ('Apple Trailers') {
        pattern {
            match $URI: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        action {
            // FIXME: temporary while MPlayer doesn't work as a downloader on Windows
            $TRANSCODER = $MENCODER
            set '-user-agent': 'QuickTime/7.6.2'
        }
    }
}
