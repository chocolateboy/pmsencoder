script {
    profile ('pmsConf') {
        pattern {
            match { pmsConf['rtmpdump.path'] == '/usr/bin/rtmpdump' }
            match { pmsConf['get-flash-videos.path'] == null }
        }

        action {
            set '-rtmpdump-path': pmsConf['rtmpdump.path']
            pmsConf['pmsconf.int'] = 42
            pmsConf['pmsconf.str'] = 'foobar'
        }
    }
}
