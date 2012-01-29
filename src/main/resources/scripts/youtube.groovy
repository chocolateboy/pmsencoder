// videofeed.Web,YouTube=http://gdata.youtube.com/feeds/base/users/freddiew/uploads?alt=rss&v=2&orderby=published
check {
    // extract metadata about the video for other profiles
    profile ('YouTube Metadata') {
        // extract the resource's video_id from the URI of the standard YouTube page
        pattern {
            match $URI: '^https?://(?:\\w+\\.)?youtube\\.com/watch\\?v=(?<youtube_video_id>[^&]+)'
        }

        action {
            // fix the URI to bypass age verification
            // make sure URI is sigilized to prevent clashes with the class
            def youtube_scrape_uri = "${$URI}&has_verified=1"

            // extract the resource's sekrit identifier ($t) from the HTML
            scrape '\\bflashvars\\s*=\\s*.+?amp;t=(?<youtube_t>.+?%3D)', [ uri: youtube_scrape_uri ]

            // extract the title and uploader ("creator") so that scripts can use them
            youtube_title = browse (uri: youtube_scrape_uri) { $('meta', name: 'title').@content }
            youtube_uploader = browse (uri: youtube_scrape_uri) {
                $('a', 'id': 'watch-userbanner').@title
            }
        }
    }

    profile ('YouTube-DL Compatible') {
        pattern {
            // match any of the sites youtube-dl supports - copied from the source
            match $URI: [
                '^gvsearch(\\d+|all)?:[\\s\\S]+',
                '^(?:http://)?(?:[a-z0-9]+\\.)?photobucket\\.com/.*[\\?\\&]current=(.*\\.flv)',
                '^(?:http://)?(?:[a-z]+\\.)?video\\.yahoo\\.com/(?:watch|network)/([0-9]+)(?:/|\\?v=)([0-9]+)(?:[#\\?].*)?',
                '^(?:https?://)?(?:\\w+\\.)?blip\\.tv(/.+)$',
                '^(?:https?://)?(?:\\w+\\.)?facebook.com/video/video.php\\?(?:.*?)v=(\\d+)(?:.*)',
                '^(https?://)?(www\\.)?escapistmagazine.com/videos/view/([^/]+)/([^/?]+)[/?]?.*$',
                '^(?:https?://)?(?:(?:www|player).)?vimeo\\.com/(?:groups/[^/]+/)?(?:videos?/)?([0-9]+)',
                '^((?:https?://)?(?:youtu\\.be/|(?:\\w+\\.)?youtube(?:-nocookie)?\\.com/)(?!view_play_list|my_playlists|artist|playlist)(?:(?:(?:v|embed|e)/)|(?:(?:watch(?:_popup)?(?:\\.php)?)?(?:\\?|#!?)(?:.+&)?v=))?)?([0-9A-Za-z_-]+)(?(1).+)?$',
                '^(?:http://)?video\\.google\\.(?:com(?:\\.au)?|co\\.(?:uk|jp|kr|cr)|ca|de|es|fr|it|nl|pl)/videoplay\\?docid=([^\\&]+).*',
                '^(?:http://)?(?:\\w+\\.)?depositfiles.com/(?:../(?#locale))?files/(.+)',
                '^(?:http://)?(?:www\\.)?metacafe\\.com/watch/([^/]+)/([^/]+)/.*',
                '^(?:http://)?(?:www\\.)?myvideo\\.de/watch/([0-9]+)/([^?/]+).*',
                '^(?:(?:(?:http://)?(?:\\w+\\.)?youtube.com/user/)|ytuser:)([A-Za-z0-9_-]+)',
                '^(?:http://)?(?:\\w+\\.)?youtube.com/(?:(?:view_play_list|my_playlists|artist|playlist)\\?.*?(p|a|list)=|user/.*?/user/|p/|user/.*?#[pg]/c/)([0-9A-Za-z]+)(?:/.*?/([0-9A-Za-z_-]+))?.*',
                '^(?i)(?:https?://)?(?:www\\.)?dailymotion\\.[a-z]{2,3}/video/([^_/]+)_([^/]+)',
                '^(:(tds|thedailyshow|cr|colbert|colbertnation|colbertreport))|(https?://)?(www\\.)?(thedailyshow|colbertnation)\\.com/full-episodes/(.*)$',
                '^ytsearch(\\d+|all)?:[\\s\\S]+',
                '^yvsearch(\\d+|all)?:[\\s\\S]+'
            ]
        }

        action {
            // XXX: keep this up-to-date
            $youtube_dl_compatible = '2011.09.18c' // version the regexes were copied from
        }
    }

    // perform the actual YouTube handling if the metadata has been extracted.
    // separating the profiles into metadata and implementation allows scripts to
    // override just this profile without having to rescrape the page to match on
    // the uploader &c.
    //
    // it also simplifies custom matchers e.g. check for 'YouTube Medatata' in $MATCHES
    // rather than repeating the regex

    profile ('YouTube-DL') {
        pattern {
            match 'YouTube-DL Compatible'
            match { PYTHON && YOUTUBE_DL }
        }

        action {
            $youtube_dl_enabled = true
            $URI = quoteURI($URI)
            if (YOUTUBE_DL_MAX_QUALITY) {
                $DOWNLOADER = "$PYTHON $YOUTUBE_DL --max-quality $YOUTUBE_DL_MAX_QUALITY --quiet -o $DOWNLOADER_OUT ${$URI}"
            } else {
                $DOWNLOADER = "$PYTHON $YOUTUBE_DL --quiet -o $DOWNLOADER_OUT ${$URI}"
            }
        }
    }

    profile ('YouTube') {
        pattern {
            // fall back to the native handler if youtube-dl is not installed/enabled
            match { $youtube_video_id && !$youtube_dl_enabled }
        }

        // Now, with $video_id and $t defined, call the builtin YouTube handler.
        // Note: the parentheses are required for a no-arg method call
        action {
            youtube()
        }
    }
}
