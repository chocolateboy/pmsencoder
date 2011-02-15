// XXX this script requires PMSEncoder >= 1.4.0
// XXX this script needs to be loaded before get_flash_videos.groovy
// FIXME: for now, the easiest way to ensure that is to save this as INIT.groovy

script {
    profile ('Megavideo') {
        pattern {
            domain 'megavideo.com'
        }

        action {
            // $PARAMS.waitbeforestart = 10000L
            set '-r': '24'
        }
    }
}
