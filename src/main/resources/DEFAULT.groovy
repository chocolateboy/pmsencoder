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
    // have to be completely silent on Windows as stdout is sent to the transcoder
    def mplayerLogLevel = $PMS.get().isWindows() ? 'all=-1' : 'all=2'
    def ICHC = 'I Can Has Cheezburger'
    def IPAD_USER_AGENT = 'Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) ' +
        'AppleWebKit/531.21.10 (KHTML, like Gecko) ' +
        'Version/4.0.4 Mobile/7B334b Safari/531.21.10'

    /*
        Default download and transcode commands e.g.

            $DOWNLOADER = "mplayer -msglevel all=2 -prefer-ipv4 -quiet -dumpstream -dumpfile $DOWNLOADER_OUT ${$URI}"
            $TRANSCODER = "ffmpeg -v 0 -y -threads nbcores -i ${$URI} -target pal-dvd $TRANSCODER_OUT"
            $TRANSCODER = "ffmpeg -v 0 -y -threads nbcores -i $DOWNLOADER_OUT -target pal-dvd $TRANSCODER_OUT"
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

    // XXX these three profiles must precede 'YouTube Metadata'
    profile (ICHC) {
        pattern {
            domain 'icanhascheezburger.com'
        }
    }

    profile ('I Can Has YouTube') {
        pattern {
            match ICHC
            scrape '\\bhttp://www\\.youtube\\.com/v/(?<video_id>[^&?]+)'
        }

        action {
            $URI = "http://www.youtube.com/watch?v=${video_id}"
        }
    }

    profile ('I Can Has Viddler') {
        pattern {
            match ICHC
            scrape "\\bsrc='(?<URI>http://www\\.viddler\\.com/file/\\w+/html5mobile/)'"
        }

        action {
            $DOWNLOADER = $MPLAYER

            downloader {
                set '-user-agent': IPAD_USER_AGENT
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

    /*
        videofeed.Web,ZDF=http://www.zdf.de/ZDFmediathek/rss/562?view=rss
        videofeed.Web,3sat=http://www.3sat.de/mediathek/rss/mediathek_scobel.xml
        videofeed.Web,3sat=http://www.3sat.de/mediathek/rss/mediathek_boerse.xml
        videofeed.Web,3sat=http://www.3sat.de/mediathek/rss/mediathek_hitec.xml
    */

    profile ('ARD/ZDF') {
        pattern {
            match $URI: '^http://hstreaming\\.(ard|zdf)\\.de/.+?\\.mov$'
        }

        action {
            // scrape the RTSP URI from the .mov "container" (actually a plain text file)
            scrape '(?<URI>rtsp://\\S+)'

            // now set the correct MPlayer option
            $DOWNLOADER = $MPLAYER

            downloader {
                set '-rtsp-stream-over-tcp' // needed for some firewalls/routers
            }
        }
    }

    // videostream.Web,TV=Bloomberg TV,http://www.bloomberg.com/streams/video/LiveBTV200.asx

    profile ('Bloomberg TV') {
        pattern {
            match { $URI == 'http://www.bloomberg.com/streams/video/LiveBTV200.asx' }
        }

        action {
            $TRANSCODER = $MENCODER
            // grab the .asx file and extract the first double-quoted MMS URI into $URI
            scrape '"(?<URI>mms://[^"]+)"'
            // preserve the low bitrate
            replace '-lavcopts': [ '4096': '238' ]
            // preserve the low framerate
            set '-ofps': 15
            // fix sync issues (these are in the stream itself)
            set '-delay': 0.2
        }
    }

    /*
        videostream.Web,TV=Bloomberg Live,mms://a627.l2479952251.c24799.g.lm.akamaistream.net/D/627/24799/v0001/reflector:52251,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
        videostream.Web,TV=Bloomberg Live stream 2,mms://a536.l2479952400.c24799.g.lm.akamaistream.net/D/536/24799/v0001/reflector:52400,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
        videostream.Web,TV=Bloomberg Live stream 3,mms://a1598.l2489858165.c24898.n.lm.akamaistream.net/D/1598/24898/v0001/reflector:58165,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
        videostream.Web,TV=Bloomberg Live stream 4,mms://a1332.l2489859148.c24898.n.lm.akamaistream.net/D/1332/24898/v0001/reflector:59148,http://www.definedbymedia.com/images/logos/logos-bloombergtelevision.jpg
    */

    profile('Bloomberg Live') {
        pattern {
            match $URI: '\\blm\\.akamaistream\\.net/D/'
        }

        action {
            // fix sync issues (these are in the stream itself)
            set '-delay': 0.2
        }
    }

    // videofeed.Web,Eurogamer=http://rss.feedsportal.com/feed/eurogamer/eurogamer_tv

    profile ('Eurogamer Redirect') {
        pattern {
            domain 'rss.feedsportal.com'
        }

        action {
            $URI = $HTTP.target($URI)
        }
    }

    profile ('Eurogamer') {
        pattern {
            domain 'eurogamer.net'
        }

        action {
            $DOWNLOADER = $MPLAYER

            downloader {
                // -referrer requires a recent-ish MEncoder (from June 2010)
                set '-referrer': $URI
            }

            $URI = 'http://www.eurogamer.net/' + browse { $('a.download').@href }
        }
    }

    /*
        videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/Latest30CinemaCreated/
        videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/Latest30InCinema/
        videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/latest30ondvd/
        videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/Next30InCinema/
        videofeed.Web,Filmtrailer=http://fr.rss.filmtrailer.com/default/Latest30CinemaCreated/
        videofeed.Web,Filmtrailer=http://fr.rss.filmtrailer.com/default/Latest30InCinema/
        videofeed.Web,Filmtrailer=http://fr.rss.filmtrailer.com/default/Next30InCinema/
        videofeed.Web,Filmtrailer=http://uk.rss.filmtrailer.com/default/Latest30CinemaCreated/
        videofeed.Web,Filmtrailer=http://uk.rss.filmtrailer.com/default/Latest30InCinema/
        videofeed.Web,Filmtrailer=http://uk.rss.filmtrailer.com/default/Next30InCinema/
    */

    // FIXME: these should be converted into ATOM feeds with multiple enclosures (clips)
    // XXX need to improve PMS's feed handling

    profile ('Filmtrailer.com') {
        def SIZES = [ 'xxlarge', 'xlarge', 'large', 'medium', 'small' ]

        pattern {
            domain 'filmtrailer.com'
        }

        action {
            // as is usual with Flash, metadata is stored in an external file - in this
            // case XML
            def flashVarsUrl = browse { $('param', name: 'FlashVars').@value.minus(~/^file=/) }

            // use HTTPBuilder to convert the XML to a GPathResult object
            def flashVars = $HTTP.getXML(flashVarsUrl)

            // assemble a list of clip URIs; typically each trailer has several
            // variants: shorter, longer, teaser, excerpt, international, interviews &c.
            def clips

            /*
                example trailer that doesn't have an xxlarge clip:

                    page: http://uk.filmtrailer.com/trailer/5644/127+hours+film+t
                    xml:  http://uk.player-feed.previewnetworks.com/v3.1/cinema/5644/441100000-1/
            */

            SIZES.any { size ->
                log.info("looking for clip size: $size")

                clips = flashVars.'**'.findAll { it.'@size' == size }.collect { it.text() }

                if (clips.size() > 0) {
                    log.info('success')
                    return true
                } else {
                    log.info('failure')
                    return false
                }
            }

            // now use the ffmpeg "concat" pseudo-protocol to join the clips together
            $URI = 'concat:' + clips.join('\\|')
        }
    }

    // videofeed.Web,Gamestar=http://www.gamestar.de/videos/rss/videos.rss

    profile ('Gamestar') {
        pattern {
            match $URI: '^http://www\\.gamestar\\.de/index\\.cfm\\?pid=\\d+&pk=\\d+'
        }

        action {
            // set the scrape URI to the URI of the XML file containing the video's metadata
            scrape '/jw4/player\\.swf\\?config=(?<URI>[^"]+)'
            // now extract the video URI from the XML's <file>...</file> element
            scrape '<file>(?<URI>[^<]+)</file>'
        }
    }

   // videofeed.Web,Gameswelt=http://www.gameswelt.de/feeds/videos/rss.xml
   profile ('Gameswelt') {
        pattern {
            match $URI: '^http://www\\.gameswelt\\.de/videos/videos/'
        }

        action {
            // extract the video URI from the value of the flashvars param
            scrape '<param\\s+name="flashvars".+(?<URI>http://video\\.gameswelt\\.de/[^&]+)'
        }
    }

    // redirect Megaupload links to Megavideo
    profile ('Megaupload') {
        pattern {
            domain 'megaupload.com'
        }

        action {
            $URI = browse (uri: $HTTP.target($URI)) { $('a.mvlink').@href }
        }
    }

    // XXX this script needs to be loaded before get_flash_videos.groovy
    profile ('Megavideo') {
        pattern {
            domain 'megavideo.com'
        }

        action {
            // $PARAMS.waitbeforestart = 10000L
            set '-r': '24'
        }
    }

    // videofeed.Web,Wimp=http://www.wimp.com/rss/
    profile ('Get Flash Videos') {
        pattern {
            match { GET_FLASH_VIDEOS != null }
            domains([ 'wimp.com', 'megavideo.com' ]) // &c.
        }

        action {
            $URI = quoteURI($URI)
            $DOWNLOADER = "$PERL $GET_FLASH_VIDEOS --quality high --quiet --yes --filename $DOWNLOADER_OUT ${$URI}"
        }
    }

    // videofeed.Web,GiantBomb=http://pipes.yahoo.com/pipes/pipe.run?_id=cf668f0d78945d30144d7d48d5021edc&_render=rss
    profile ('GiantBomb') {
        pattern {
            domain 'giantbomb.com'
            scrape '"streaming_\\w+":\\s*"(?<extension>\\w+):(?<path>[^"]+)"'
        }

        action {
            $URI = "http://media.giantbomb.com/${path}.${extension}"
        }
    }

    profile ('HTTP Live Stream') {
        pattern {
            match $URI: '\\.m3u8$'
            match { HLS_PLAYER != null }
        }

        action {
            $URI = quoteURI($URI)
            $DOWNLOADER = "$PYTHON $HLS_PLAYER --path $DOWNLOADER_OUT ${$URI}"
        }
    }

    // videofeed.Web,PCGames=http://videos.pcgames.de/rss/newest.php
    profile ('PC Games') {
        pattern {
            domain 'pcgames.de'
        }

        action {
            $URI = $HTTP.target($URI)
            scrape '\\bcurrentposition\\s*=\\s*(?<currentPosition>\\d+)\\s*;'
            scrape "'(?<URI>http://\\w+\\.gamereport\\.de/videos/[^']+?/${currentPosition}/[^']+)'"
        }
    }

    // videostream.Web,TV=Politiek 24,http://livestreams.omroep.nl/nos/politiek24-bb,http://assets.www.omroep.nl/system/files/2140/thumbnail/Politiek_24.jpg
    profile ('Politiek 24') {
        pattern {
            match { $URI == 'http://livestreams.omroep.nl/nos/politiek24-bb' }
        }

        action {
            // grab the .asx file and extract the first stream into $URI
            scrape '<Ref\\s+href="(?<URI>[^"]+)"\\s*/>'
        }
    }

    profile ('SopCast') {
        pattern {
            protocol 'sop'
            match { SOPCAST != null }
        }

        action {
            $URI = quoteURI($URI)
            $HOOK = "$SOPCAST ${$URI}"
            $URI = 'http://127.0.0.1:8902/stream'
        }
    }

    // videofeed.Web,WinFuture=http://rss.feedsportal.com/c/617/f/448481/index.rss
    profile ('Feeds Portal Redirect') {
        pattern {
            domain 'rss.feedsportal.com'
        }

        action {
            $URI = $HTTP.target($URI)
        }
    }

    profile ('WinFuture') {
        pattern {
            domain 'winfuture.de'
        }

        action {
            // extract the script path from the HTML
            scrape '<script\\s+src="(?<path>/video/video\\.php\\?video_id=\\d+&amp;autostart)"'
            // now grab the URI from the script
            scrape "wfv\\d+_flowplayer_init\\(\\s*\\d+,\\s*'(?<escaped>[^']+)'", [ uri: "http://winfuture.de${path}" ]
            // and 1) unescape it 2) resolve redirects (to work around a bug in MEncoder/MPlayer's HTTP support):
            // XXX 2) may not be needed for ffmpeg
            // http://lists.mplayerhq.hu/pipermail/mplayer-dev-eng/2010-December/067084.html
            $URI = $HTTP.target(URLDecoder.decode($escaped))
        }
    }

    if (PYTHON != null && YOUTUBE_DL != null) {
        // videofeed.Web,YouTube=http://gdata.youtube.com/feeds/base/users/freddiew/uploads?alt=rss&v=2&orderby=published
        profile ('YouTube-DL', replaces: 'YouTube') { // replace it with a profile that works for all YouTube-DL sites
            pattern {
                match 'YouTube-DL Compatible' // built-in profile; matches if the site is supported by youtube-dl
            }

            action {
                def maxQuality = YOUTUBE_DL_MAX_QUALITY ?: 22
                $URI = quoteURI($URI)
                $DOWNLOADER = "$PYTHON $YOUTUBE_DL --max-quality $maxQuality --quiet -o $DOWNLOADER_OUT ${$URI}"
            }
        }
    }

    profile ('Unsupported FFmpeg Protocol') {
        pattern {
            match {
                // http://www.ffmpeg.org/ffmpeg-doc.html#SEC33
                !($PROTOCOL in [
                    'file',
                    'gopher',
                    'http',
                    'pipe',
                    'rtmp',
                    'rtmpt',
                    'rtmpe',
                    'rtmpte',
                    'rtmps',
                    'rtp',
                    'tcp',
                    'udp',
                    'concat'
                ])
            }
        }

        action {
            if ($DOWNLOADER == null) {
                $DOWNLOADER = $MPLAYER
            }
        }
    }
}
