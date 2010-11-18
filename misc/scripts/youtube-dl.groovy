config {
    def YOUTUBE_DL = '/path/to/youtube-dl'
    def PYTHON = '/usr/bin/python'
    def MENCODER = $PMS.getConfiguration().getMencoderPath()

    profile ('YouTube-DL', replaces: 'YouTube') { // replace it with a profile that works for all YouTube-DL sites
        pattern {
            match { 'YouTube-DL Compatible' in $MATCHES }
        }

        action {
            $EXECUTABLE = 'SHELL'
            $ARGS = "$PYTHON $YOUTUBE_DL --quiet --max-quality 37 -o - \"${$URI}\" | $MENCODER - ".tokenize() + $ARGS
        }
    }
}
