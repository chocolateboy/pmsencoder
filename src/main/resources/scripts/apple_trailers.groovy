script {
    profile ('Apple Trailers') {
        pattern {
            match $URI: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        action {
            $DOWNLOADER = $MPLAYER

            downloader {
                set '-user-agent': 'QuickTime/7.6.2'
            }
        }
    }
}
