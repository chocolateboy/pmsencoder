/*
    this is the default/builtin PMSEncoder script (i.e. configuration file). PMSEncoder loads it from
    src/main/resources/pmsencoder.groovy, but it's also symlinked, for reference, from
    scripts/pmsencoder.groovy

    see:

       http://github.com/chocolateboy/pmsencoder/blob/plugin/src/main/resources/pmsencoder.groovy
       http://github.com/chocolateboy/pmsencoder/blob/plugin/scripts/pmsencoder.groovy

    XXX: Don't use this as a tutorial/documentation; see the wiki instead.
    XXX: The scripting framework/DSL is constantly changing, so don't rely on anything here.
*/

config {
    def nbcores = $PMS.getConfiguration().getNumberOfCpuCores()

    // the default MEncoder args - these can be redefined in a script
    $DEFAULT_MENCODER_ARGS = [
        '-prefer-ipv4',
        '-oac', 'lavc',
        '-of', 'lavf',
        '-lavfopts', 'format=dvd',
        '-ovc', 'lavc',
        '-lavcopts', "vcodec=mpeg2video:vbitrate=4096:threads=${nbcores}:acodec=ac3:abitrate=128",
        '-ofps', '25',
        '-cache', '16384', // default cache size; default minimum percentage is 20%
        '-vf', 'harddup'
    ]

    /*
        this is the default list of YouTube format/resolution IDs we should accept/select - in descending
        order of preference.

        it can be modified globally (in a script) to add/remove a format, or can be overridden on
        a per-video basis by supplying a new list to the youtube method (see below) e.g.

        exclude '1080p':

            youtube $YOUTUBE_ACCEPT - [ 37 ]

        add '2304p':

            youtube [ 38 ] + $YOUTUBE_ACCEPT

        For the full list of formats, see: http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    */

    $YOUTUBE_ACCEPT = [
        37,  // 1080p
        22,  // 720p
        35,  // 480p
        34,  // 360p
        18,  // Medium
        5    // 240p
    ]

    /*
        this is placed here (i.e. first) as a convenience so that scripts can create/override
        settings common to all other profiles without modifying $DEFAULT_MENCODER_ARGS e.g.:

            // set the default audio bitrate to 348 Kbps
            // (see src/test/resources/profile_default.groovy)

            profile ('Default') {
                action {
                    tr '-lavcopts': [ 'abitrate=\\d+': 'abitrate=384' ]
                }
            }
    */

    profile ('Default') {
        pattern { match { false } }
    }

    profile ('YouTube-DL Compatible') {
        pattern {
            // match any of the sites youtube-dl supports - copied from the source
            match $URI: [
                '^((?:https?://)?(?:youtu\\.be/|(?:\\w+\\.)?youtube(?:-nocookie)?\\.com/' +
                '(?:(?:v/)|(?:(?:watch(?:_popup)?(?:\\.php)?)?(?:\\?|#!?)(?:.+&)?v=))))?([0-9A-Za-z_-]+)(?(1).+)?$',
                '(?:http://)?(?:www\\.)?metacafe\\.com/watch/([^/]+)/([^/]+)/.*',
                '(?i)(?:https?://)?(?:www\\.)?dailymotion\\.[a-z]{2,3}/video/([^_/]+)_([^/]+)',
                '(?:http://)?video\\.google\\.(?:com(?:\\.au)?|co\\.(?:uk|jp|kr|cr)|ca|de|es|fr|it|nl|pl)' +
                    '/videoplay\\?docid=([^\\&]+).*',
                '(?:http://)?(?:[a-z0-9]+\\.)?photobucket\\.com/.*[\\?\\&]current=(.*\\.flv)',
                '(?:http://)?(?:[a-z]+\\.)?video\\.yahoo\\.com/(?:watch|network)/([0-9]+)(?:/|\\?v=)([0-9]+)(?:[#\\?].*)?',
                '(?:http://)?(?:\\w+\\.)?youtube.com/(?:(?:view_play_list|my_playlists)\\?.*?p=|user/.*?/user/)([^&]+).*',
                '(?:http://)?(?:\\w+\\.)?youtube.com/user/(.*)'
            ]
        }

        action {
            $youtube_dl_compatible = '2010.10.03' // version the regexes were copied from
        }
    }

    // extract metadata about the video for other profiles
    profile ('YouTube Metadata') {
        // extract the resource's video_id from the URI of the standard YouTube page
        pattern {
            match $URI: '^http://(?:\\w+\\.)?youtube\\.com/watch\\?v=(?<youtube_video_id>[^&]+)'
        }

        action {
            // fix the URI to bypass age verification
            def youtube_scrape_uri = "${$URI}&has_verified=1"

            // extract the resource's sekrit identifier ($t) from the HTML
            // make sure URI is sigilized to prevent clashes with the class
            scrape '&t=(?<youtube_t>[^&]+)', [ uri: youtube_scrape_uri ]

            // extract the uploader ("creator") so that scripts can use it
            // scrape '"creator"\\s*:\\s*"(?<youtube_creator>[^"]+)"', [ uri: youtube_scrape_uri ]
            scrape "'VIDEO_USERNAME'\\s*:\\s*'(?<youtube_uploader>[^']+)'", [ uri: youtube_scrape_uri ]
        }
    }

    // perform the actual YouTube handling if the metadata has been extracted.
    // separating the profiles into metadata and implementation allows scripts to
    // override just this profile without having to rescrape the page to match on
    // the uploader &c.
    //
    // it also simplifies custom matchers e.g. check for 'YouTube Medatata' in $MATCHES
    // rather than repeating the regex

    profile ('YouTube') {
        pattern {
            match { $youtube_video_id != null }
        }

        // Now, with $video_id and $t defined, call the builtin YouTube handler.
        // Note: the parentheses are required for a no-arg action
        action {
            youtube()
        }
    }

    profile ('Apple Trailers') {
        pattern {
            match $URI: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        // FIXME: the default 4096 kbps (1/2 a megabyte per second) video bitrate
        // is needlessly high; these typically weigh in at ~1200 kbps
        action {
            set '-ofps': '24', '-user-agent': 'QuickTime/7.6.2'
        }
    }

    profile ('Apple Trailers HD') {
        pattern {
            match { 'Apple Trailers' in $MATCHES }
            match $URI: '(_h720p\\.mov|\\.m4v)$'
        }

        action {
            tr '-lavcopts': [ '4096': '5086' ] // increase the bitrate
        }
    }

    profile ('TED') {
        pattern {
            match $URI: '^http://feedproxy\\.google\\.com/~r/TEDTalks_video\\b'
        }

        action {
            set '-ofps': '24'
        }
    }

    profile ('GameTrailers (Revert PMS Workaround)') {
        /*
           convert:

               http://www.gametrailers.com/download/48298/t_ufc09u_educate_int_gt.flv

           to:

               http://www.gametrailers.com/player/48298.html
         */

        // 1) extract the page ID
        pattern {
            match $URI: '^http://(www\\.)?gametrailers\\.com/download/(?<gametrailers_page_id>\\d+)/[^.]+\\.flv$'
        }

        // 2) and use it to restore the correct web page URI
        action {
           let $URI: "http://www.gametrailers.com/player/${gametrailers_page_id}.html"
        }
    }

    profile ('GameTrailers') {
        pattern {
            match $URI: '^http://(www\\.)?gametrailers\\.com/'
        }

        action {
            /*
                The order is important here! Make sure we scrape the variables before we set the URI.
                extract some values from the HTML
            */
            scrape '\\bmov_game_id\\s*=\\s*(?<gametrailers_movie_id>\\d+)'
            scrape '\\bhttp://www\\.gametrailers\\.com/download/\\d+/(?<gametrailers_filename>t_[^.]+)\\.wmv\\b'

            // now use them to rewrite the URI
            $URI = "http://trailers-ak.gametrailers.com/gt_vault/${gametrailers_movie_id}/${gametrailers_filename}.flv"
        }
    }
}
