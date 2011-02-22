// videofeed.Web,Eurogamer=http://rss.feedsportal.com/feed/eurogamer/eurogamer_tv

script {
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
