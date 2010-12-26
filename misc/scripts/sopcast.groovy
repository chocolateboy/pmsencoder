// this is a tweaked version of a script by hans_gregor:
// http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776&p=41615#p41612
// change /path/to/sopcast to the path to the sopcast binary to use it

script {
    def SURI = 'SOPCAST_URI'
    def SLOO = 'SOPCAST_LOOPBACK'
    def SPLA = 'SOPCAST_PLAY'

    profile (SURI) {
        def sopcast = '/path/to/sopcast'

        pattern {
            protocol 'sop'
        }

        action {
            $DOWNLOADER = "$sopcast ${$URI}"
        }
    }

    profile (SLOO, after: SURI) {
        pattern {
            match SURI
        }

        action {
            $URI = 'http://127.0.0.1:8902/stream'
        }
    }

    profile (SPLA, after: SLOO) {
        pattern {
            match SLOO
        }

        action {
            set '-lavcopts': 'vcodec=mpeg2video:acodec=mp2:abitrate=128', '-ofps': '24'
            $DOWNLOADER_OUT = $URI
        }
    }
}
