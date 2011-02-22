// XXX this script needs to be loaded before get_flash_videos.groovy (currently a check script)

script {
    // redirect Megaupload links to Megavideo
    profile ('Megaupload') {
        pattern {
            domain 'megaupload.com'
        }

        action {
            $URI = browse (uri: $HTTP.target($URI)) { $('a.mvlink').@href }
        }
    }

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
