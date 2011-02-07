// XXX this script requires PMSEncoder >= 1.4.0
// TODO: script (before: 'get_flash_videos') { ... }

script {
    profile ('MEGAUPLOAD') {
        pattern {
            domain 'megaupload.com'
        }

        action {
            $URI = browse (uri: $HTTP.target($URI)) { $('a.mvlink').@href }
        }
    }

    profile ('MEGAVIDEO') {
        pattern {
            domain 'megavideo.com'
        }

        action {
            $PARAMS.waitbeforestart = 10000L
            $TRANSCODER = "$FFMPEG -y -loglevel info -r 24 -i $DOWNLOADER_OUT -target ntsc-dvd $TRANSCODER_OUT"
        }
    }
}
