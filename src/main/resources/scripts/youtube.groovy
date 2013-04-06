// videofeed.Web,YouTube=http://gdata.youtube.com/feeds/base/users/freddiew/uploads?alt=rss&v=2&orderby=published
check {
    // extract the YouTube ID from the URL and make it available to other profiles
    profile ('YouTube ID') {
        // extract the resource's video_id from the URI of the standard YouTube page
        pattern {
            match uri: '^https?://(?:\\w+\\.)?youtube\\.com/watch\\?v=(?<youtube_video_id>[^&]+)'
        }

        action {
            // fix the URI to bypass age verification
            def youtube_scrape_uri = "${uri}&has_verified=1"

            // extract the resource's sekrit identifier (t) from the HTML
            scrape(uri: youtube_scrape_uri)('\\bflashvars\\s*=\\s*.+?amp;t=(?<youtube_t>.+?%3D)')

            // extract the title and uploader ("creator") so that scripts can use them
            def $j = $(uri: youtube_scrape_uri) // curry
            youtube_title = $j('meta[name=title]').attr('content')
            youtube_uploader = $j('a[id=watch-userbanner]').attr('title')
        }
    }

    profile ('YouTube-DL Compatible') {
        pattern {
            // match any of the sites youtube-dl supports - copied from the source
            match uri: [
                '^gvsearch(\\d+|all)?:[\\s\\S]+',
                '^(?:http://)?(?:[a-z0-9]+\\.)?photobucket\\.com/.*[\\?\\&]current=(.*\\.flv)',
                '^(?:http://)?(?:[a-z]+\\.)?video\\.yahoo\\.com/(?:watch|network)/([0-9]+)(?:/|\\?v=)([0-9]+)(?:[#\\?].*)?',
                '^(?:https?://)?openclassroom.stanford.edu(/?|(/MainFolder/(?:HomePage|CoursePage|VideoPage)\\.php([?]course=([^&]+)(&video=([^&]+))?(&.*)?)?))$',
                '^(?:https?://)?(?:\\w+\\.)?blip\\.tv(/.+)$',
                '^(?:(?:(?:https?://)?(?:\\w+\\.)?blip\\.tv/)|bliptvuser:)([^/]+)/*$',
                '^(?:https?://)?(?:\\w+\\.)?facebook\\.com/(?:video/video|photo)\\.php\\?(?:.*?)v=(\\d+)(?:.*)',
                '^(?:https?://)?(?:www\\.)?collegehumor\\.com/video/([0-9]+)/(.*)$',
                '^(https?://)?(www\\.)?escapistmagazine\\.com/videos/view/([^/]+)/([^/?]+)[/?]?.*$',
                '^(?:https?://)?(?:www\\.)?infoq\\.com/[^/]+/[^/]+$',
                '^(?:https?://)?(?:www\\.)?mixcloud\\.com/([\\w\\d-]+)/([\\w\\d-]+)',
                '^(https?://)?(?:www\\.)?mtv\\.com/videos/[^/]+/([0-9]+)/[^/]+$',
                '^(?:https?://)?(?:(?:www|player).)?vimeo\\.com/(?:groups/[^/]+/)?(?:videos?/)?([0-9]+)',
                '^(?:https?://)?(?:www\\.)?soundcloud\\.com/([\\w\\d-]+)/([\\w\\d-]+)',
                '^(?:https?://)?(?:www\\.)?xvideos\\.com/video([0-9]+)(?:.*)',
                '^(?:https?://)?(?:\\w+\\.)?youtube\\.com/(?:(?:course|view_play_list|my_playlists|artist|playlist)\\?.*?(p|a|list)=|user/.*?/user/|p/|user/.*?#[pg]/c/)(?:PL)?([0-9A-Za-z-_]+)(?:/.*?/([0-9A-Za-z_-]+))?.*',
                '^(?:(?:(?:https?://)?(?:\\w+\\.)?youtube\\.com/user/)|ytuser:)([A-Za-z0-9_-]+)',
                '^((?:https?://)?(?:youtu\\.be/|(?:\\w+\\.)?youtube(?:-nocookie)?\\.com/|tube\\.majestyc\\.net/)(?!view_play_list|my_playlists|artist|playlist)(?:(?:(?:v|embed|e)/)|(?:(?:watch(?:_popup)?(?:\\.php)?)?(?:\\?|#!?)(?:.+&)?v=))?)?([0-9A-Za-z_-]+)(?(1).+)?$',
                '^(?:http://)?video\\.google\\.(?:com(?:\\.au)?|co\\.(?:uk|jp|kr|cr)|ca|de|es|fr|it|nl|pl)/videoplay\\?docid=([^\\&]+).*',
                '^http://video\\.xnxx\\.com/video([0-9]+)/(.*)',
                '^(?:http://)?v\\.youku\\.com/v_show/id_([A-Za-z0-9]+)\\.html',
                '^(?:http://)?(?:\\w+\\.)?depositfiles\\.com/(?:../(?#locale))?files/(.+)',
                '^(?:http://)?(?:www\\.)?metacafe\\.com/watch/([^/]+)/([^/]+)/.*',
                '^(?:http://)?(?:www\\.)?myvideo\\.de/watch/([0-9]+)/([^?/]+).*',
                '^(?i)(?:https?://)?(?:www\\.)?dailymotion\\.[a-z]{2,3}/video/([^_/]+)_([^/]+)',
                '^(:(tds|thedailyshow|cr|colbert|colbertnation|colbertreport))|(https?://)?(www\\.)?(thedailyshow|colbertnation)\\.com/full-episodes/(.*)$',
                '^ytsearch(\\d+|all)?:[\\s\\S]+',
                '^yvsearch(\\d+|all)?:[\\s\\S]+'
            ]
        }

        action {
            // XXX: keep this up-to-date
            youtube_dl_compatible = '2012.09.27' // version the regexes were copied from
        }
    }

    // perform the actual YouTube handling if the metadata has been extracted.
    // separating the profiles into metadata and implementation allows scripts to
    // override just this profile without having to rescrape the page to match on
    // the uploader &c.
    //
    // it also simplifies custom matchers e.g. check for 'YouTube Medatata' in matches
    // rather than repeating the regex

    profile ('YouTube-DL') {
        def YOUTUBE_DL_PATH

        if (YOUTUBE_DL) {
            if ((new File(YOUTUBE_DL)).canExecute()) {
                YOUTUBE_DL_PATH = YOUTUBE_DL
            } else if (PYTHON) {
                YOUTUBE_DL_PATH = "$PYTHON $YOUTUBE_DL"
            }
        }

        pattern {
            match 'YouTube-DL Compatible'
            match { YOUTUBE_DL_PATH }
        }

        action {
            youtube_dl_enabled = true
            uri = quoteURI(uri)

            if (YOUTUBE_DL_MAX_QUALITY) {
                downloader = "$YOUTUBE_DL_PATH --max-quality $YOUTUBE_DL_MAX_QUALITY --quiet -o $DOWNLOADER_OUT ${uri}"
            } else {
                downloader = "$YOUTUBE_DL_PATH --quiet -o $DOWNLOADER_OUT ${uri}"
            }
        }
    }

    profile ('YouTube') {
        pattern {
            // fall back to the native handler if youtube-dl is not installed/enabled
            match { youtube_video_id && !youtube_dl_enabled }
        }

        // Now, with video_id and t defined, call the builtin YouTube handler.
        // Note: the parentheses are required for a no-arg method call
        action {
            youtube()
        }
    }
}
