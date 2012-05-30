// videofeed.Web,Eurogamer=http://rss.feedsportal.com/feed/eurogamer/eurogamer_tv

script {
    profile ('Eurogamer') {
        pattern {
            domain 'eurogamer.net'
        }

        action {
            $URI = $HTTP.target($URI)

            if (FFMPEG_HTTP_HEADERS) {
                set '-headers': 'Referer: ' + $URI
            } else {
                $TRANSCODER = $MENCODER
                // -referrer requires a recent-ish MEncoder (>= June 2010)
                set '-referrer': $URI
            }

            // http://www.eurogamer.net/videos/sc2-heart-of-the-swarm-blizzcon-trailer
            // http://www.eurogamer.net/tv/playlist/110585
            // http://videos.eurogamer.net/47006bb3917b8da6218d491d43493033/500ae168/starcraftiiheartoftheswarmpreviewtrailernewunits-ibuvff6xrkm_stream_h264v2_hd.mp4
            $URI = 'http://videos.eurogamer.net/' + browse { $('a.download').@href }
        }
    }
}
