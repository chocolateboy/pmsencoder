script {
    profile ('Apple Trailers') {
        pattern {
            match uri: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        action {
            set '-user-agent': 'QuickTime/7.6.2'
        }
    }
}
