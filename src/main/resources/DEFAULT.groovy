/*
    this is the default/builtin PMSEncoder script. PMSEncoder loads it from
    src/main/resources/DEFAULT.groovy

    see:

       http://github.com/chocolateboy/pmsencoder/blob/plugin/src/main/resources/DEFAULT.groovy

    XXX: Don't use this as a tutorial/documentation; see the wiki instead.
    XXX: The scripting framework/DSL is constantly changing, so don't rely on anything here.
*/

script {
    def nbcores = $PMS.getConfiguration().getNumberOfCpuCores()

    /*
        Default download and transcode commands e.g.

            $DOWNLOADER = "mplayer -msglevel all=2 -prefer-ipv4 -quiet -dumpstream -dumpfile $DOWNLOADER_OUT ${$URI}"
            $TRANSCODER = "ffmpeg -v 0 -y -threads nbcores -i ${$URI} -target ntsc-dvd $TRANSCODER_OUT"
            $TRANSCODER = "ffmpeg -v 0 -y -threads nbcores -i $DOWNLOADER_OUT -target ntsc-dvd $TRANSCODER_OUT"
            $TRANSCODER = "mencoder -mencoder -options -o $TRANSCODER_OUT ${$URI}"
            $TRANSCODER = "mencoder -mencoder -options -o $TRANSCODER_OUT $DOWNLOADER_OUT"

        By default, ffmpeg is used without a separate downloader.

        If a default downloader/transcoder is used, then PMSEncoder adds the appropriate
        input/output options as shown above. Otherwise these must be set manually.

        Note: the uppercase executable names (e.g. FFMPEG) are used to signal to PMSEncoder.groovy that the
        configured path should be substituted.
    */

    // default ffmpeg transcode command - can be redefined in a userscript
    $FFMPEG = [
        'FFMPEG',
        '-v', '0',
        '-y',
        '-threads', nbcores
    ]

    // default mencoder transcode command - can be redefined in a userscript
    $MENCODER = [
        'MENCODER', // XXX add support for mencoder-mt
        '-msglevel', 'all=2',
        '-quiet',
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

    // have to be completely silent on Windows as stdout is sent to the transcoder
    def mplayerLogLevel = $PMS.get().isWindows() ? 'all=-1' : 'all=2'

    // default mplayer download command - can be redefined in a userscript
    $MPLAYER = [
        'MPLAYER',
        '-msglevel', mplayerLogLevel,
        '-quiet',
        '-prefer-ipv4',
        '-dumpstream'
    ]

    /*
        this is the default list of YouTube format/resolution IDs we should accept/select - in descending
        order of preference.

        it can be modified globally (in a script) to add/remove a format, or can be overridden on
        a per-video basis by supplying a new list to the youtube method (see below) e.g.

        exclude '1080p':

            youtube $YOUTUBE_ACCEPT - [ 37 ]

        add '2304p':

            youtube([ 38 ] + $YOUTUBE_ACCEPT)

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

    profile ('pmsencoder://') {
        pattern {
            protocol 'pmsencoder'
        }

        // currently three values can be set
        // the URI (required)
        // the referrer (optional)
        // the user-agent (optional)
        action {
            def pairs = URLEncodedUtils.parse($URI)
            def setDownloaderArg = { key, value ->
                if ($DOWNLOADER == null) {
                    $DOWNLOADER = $MPLAYER + [ key, value ]
                } else if ($DOWNLOADER.size() > 0 && $DOWNLOADER[0] == 'MPLAYER') {
                    downloader {
                        set([ (key): value ])
                    }
                }
            }

            for (pair in pairs) {
                def name = pair.name
                def value = pair.value
                switch (name) {
                    case 'uri':
                        $URI = URLDecoder.decode(value)
                        break
                    case 'referrer':
                        // this requires a recent (post June 2010) MPlayer
                        setDownloaderArg('-referrer', URLDecoder.decode(value))
                        break
                    case 'user_agent':
                        setDownloaderArg('-user-agent', URLDecoder.decode(value))
                        break
                }
            }
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
            // make sure URI is sigilized to prevent clashes with the class
            def youtube_scrape_uri = "${$URI}&has_verified=1"

            // extract the resource's sekrit identifier ($t) from the HTML
            scrape '&t=(?<youtube_t>[^&]+)', [ uri: youtube_scrape_uri ]

            // extract the title and uploader ("creator") so that scripts can use them
            youtube_title = browse (uri: youtube_scrape_uri) { $('meta', name: 'title').@content }
            youtube_uploader = browse (uri: youtube_scrape_uri) {
                $('span', 'data-subscription-type': 'user').'@data-subscription-username'
            }
        }
    }

    profile ('YouTube-DL Compatible') {
        pattern {
            // match any of the sites youtube-dl supports - copied from the source
            match $URI: [
                '^(?:http://)?(?:[a-z0-9]+\\.)?photobucket\\.com/.*[\\?\\&]current=(.*\\.flv)',
                '^(?:http://)?(?:[a-z]+\\.)?video\\.yahoo\\.com/(?:watch|network)/([0-9]+)(?:/|\\?v=)([0-9]+)(?:[#\\?].*)?',
                '^((?:https?://)?(?:youtu\\.be/|(?:\\w+\\.)?youtube(?:-nocookie)?\\.com/(?:(?:v/)|(?:(?:watch(?:_popup)?(?:\\.php)?)?(?:\\?|#!?)(?:.+&)?v=))))?([0-9A-Za-z_-]+)(?(1).+)?$',
                '^(?:http://)?video\\.google\\.(?:com(?:\\.au)?|co\\.(?:uk|jp|kr|cr)|ca|de|es|fr|it|nl|pl)/videoplay\\?docid=([^\\&]+).*',
                '^(?:http://)?(?:\\w+\\.)?depositfiles.com/(?:../(?#locale))?files/(.+)',
                '^(?:http://)?(?:www\\.)?metacafe\\.com/watch/([^/]+)/([^/]+)/.*',
                '^(?:http://)?(?:\\w+\\.)?youtube.com/user/(.*)',
                '^(?:http://)?(?:\\w+\\.)?youtube.com/(?:(?:view_play_list|my_playlists)\\?.*?p=|user/.*?/user/)([^&]+).*',
                '^(?i)(?:https?://)?(?:www\\.)?dailymotion\\.[a-z]{2,3}/video/([^_/]+)_([^/]+)'
            ]
        }

        action {
            // XXX: keep this up-to-date
            $youtube_dl_compatible = '2010.12.09' // version the regexes were copied from
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
            $DOWNLOADER = $MPLAYER

            downloader {
                set '-user-agent': 'QuickTime/7.6.2'
            }
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
           $URI = "http://www.gametrailers.com/player/${gametrailers_page_id}.html"
        }
    }

    // videofeed.Web,Test,GameTrailers=http://www.gametrailers.com/rssgenerate.php?s1=&favplats[ps3]=ps3&quality[hd]=on&agegate[no]=on&orderby=newest&limit=100
    profile ('GameTrailers') {
        pattern {
            domain 'gametrailers.com'
        }

        action {
            def gmi = scrape('\\bmov_game_id\\s*=\\s*(?<gametrailers_movie_id>\\d+)')
            def gfl = scrape('\\bhttp://www\\.gametrailers\\.com/download/\\d+/(?<gametrailers_filename>t_[^.]+)\\.wmv\\b')

            if (gmi && gfl) {
                $URI = "http://trailers-ak.gametrailers.com/gt_vault/${gametrailers_movie_id}/${gametrailers_filename}.flv"
            } else if (scrape('\\bvar\\s+mov_id\\s*=\\s*(?<gametrailers_mov_id>\\d+)')) {
                def scrapeURI = "http://www.gametrailers.com/neo/?page=xml.mediaplayer.Mediagen&movieId=${gametrailers_mov_id}&hd=1"
                scrape '<src>\\s*(?<URI>\\S+)\\s*</src>', [ uri: scrapeURI ]
            }
        }
    }

    profile ('Onion News Network') {
        pattern {
            domain 'theonion.com'
        }

        action {
            // chase redirects (if possible)
            $URI = $HTTP.target($URI) ?: $URI
        }
    }
}
