// videofeed.Web,Eurogamer=http://rss.feedsportal.com/feed/eurogamer/eurogamer_tv

// XXX this script requires PMSEncoder >= 1.4.0

script {
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
}
