script {
    profile('pmsConf') {
        pattern {
            match { pmsConf['rtmpdump.path'] == '/usr/bin/rtmpdump' }
            match { pmsConf['get-flash-videos.path'] == null }
        }

        action {
            set '-rtmpdump-path': pmsConf['rtmpdump.path']
        }
    }
}
