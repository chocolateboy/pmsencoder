// XXX this script requires PMSEncoder >= 1.4.1
// XXX this script needs to be loaded last
// the easiest way to ensure that is to save this as END.groovy

script {
    profile ('FFMpeg') {
        pattern {
            match([]) // only match if nothing else matches
        }

        action {
            $DOWNLOADER = "$MPLAYER -prefer-ipv4 -quiet -dumpstream -dumpfile $DOWNLOADER_OUT ${$URI}"
            $TRANSCODER = "$FFMPEG -v 0 -y -i $DOWNLOADER_OUT -target ntsc-dvd $TRANSCODER_OUT"
        }
    }
}
