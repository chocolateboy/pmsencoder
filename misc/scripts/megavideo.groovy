// XXX this script needs to be loaded before get_flash_videos.groovy
// FIXME: for now, the easiest way to ensure that is to save this as INIT.groovy

script {
    profile ('Megavideo') {
        pattern {
            domain 'megavideo.com'
        }

        action {
            // $PARAMS.waitbeforestart = 10000L
            // XXX -loglevel doesn't work on Ubuntu's ffmpeg
            $TRANSCODER = "$FFMPEG -v 0 -y -r 24 -i $DOWNLOADER_OUT -target ntsc-dvd $TRANSCODER_OUT"
        }
    }
}
