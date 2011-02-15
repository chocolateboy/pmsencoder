// videofeed.Web,YouTube=http://gdata.youtube.com/feeds/base/users/freddiew/uploads?alt=rss&v=2&orderby=published

script {
    profile ('YouTube-DL', replaces: 'YouTube') { // replace it with a profile that works for all YouTube-DL sites
        pattern {
            match 'YouTube-DL Compatible' // built-in profile; matches if the site is supported by youtube-dl
            match { PYTHON != null && YOUTUBE_DL != null }
        }

        action {
            def maxQuality = YOUTUBE_DL_MAX_QUALITY ?: 22
            $DOWNLOADER = "$PYTHON $YOUTUBE_DL --max-quality $maxQuality --quiet -o $DOWNLOADER_OUT \"${$URI}\""
        }
    }
}
