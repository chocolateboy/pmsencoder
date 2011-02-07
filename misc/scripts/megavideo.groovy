// XXX this script requires PMSEncoder >= 1.4.0
// TODO: script (before: 'get_flash_videos') { ... }

script {
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
            $PARAMS.waitbeforestart = 10000L
            // XXX -loglevel doesn't work on Ubuntu's ffmpeg
            $TRANSCODER = "$FFMPEG -v 0 -y -r 24 -i $DOWNLOADER_OUT -target ntsc-dvd $TRANSCODER_OUT"
        }
    }
}
