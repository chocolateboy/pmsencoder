// this needs to precede 'YouTube Metadata' i.e. it needs to be in a BEGIN script

script {
    def ICHC = 'I Can Has Cheezburger'

    profile (ICHC) {
        pattern {
            domain 'icanhascheezburger.com'
        }
    }

    profile ('I Can Has YouTube') {
        pattern {
            match ICHC
            scrape '\\bhttp://www\\.youtube\\.com/v/(?<video_id>[^&?]+)'
        }

        action {
            $URI = "http://www.youtube.com/watch?v=${video_id}"
        }
    }

    profile ('I Can Has Viddler') {
        pattern {
            match ICHC
            match { IPAD_USER_AGENT }
            scrape "\\bsrc='(?<URI>http://www\\.viddler\\.com/file/\\w+/html5mobile/)'"
        }

        action {
            if (FFMPEG_HTTP_HEADERS) {
                set '-headers': 'User-Agent: ' + IPAD_USER_AGENT
            } else {
                $TRANSCODER = $MENCODER
                set '-user-agent': IPAD_USER_AGENT
            }
        }
    }
}
