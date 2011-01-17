// videofeed.Web,YouTube=http://gdata.youtube.com/feeds/base/users/freddiew/uploads?alt=rss&v=2&orderby=published

script {
    def PYTHON = '/usr/bin/python'
    def YOUTUBE_DL = '/usr/bin/youtube-dl'
    def MAX_QUALITY = 22 // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs

    profile ('YouTube-DL', replaces: 'YouTube') { // replace it with a profile that works for all YouTube-DL sites
        pattern {
            match 'YouTube-DL Compatible' // built-in profile; matches if the site is supported by youtube-dl
        }

        action {
            $DOWNLOADER = "$PYTHON $YOUTUBE_DL --max-quality $MAX_QUALITY --quiet -o $DOWNLOADER_OUT ${$URI}"
        }
    }
}
