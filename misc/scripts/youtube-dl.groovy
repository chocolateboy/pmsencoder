script {
    def PYTHON = '/usr/bin/python'
    def YOUTUBE_DL = '/path/to/youtube-dl'

    profile ('YouTube-DL', replaces: 'YouTube') { // replace it with a profile that works for all YouTube-DL sites
        pattern {
            match 'YouTube-DL Compatible' // built-in profile; matches if the site is supported by youtube-dl
        }

        action {
            $DOWNLOADER = "$PYTHON $YOUTUBE_DL --max-quality 37 -o $DOWNLOADER_OUT ${$URI}"
        }
    }
}
