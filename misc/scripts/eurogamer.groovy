// videofeed.Web,Eurogamer=http://rss.feedsportal.com/feed/eurogamer/eurogamer_tv
script {
    profile ('Redirect') { // create or replace
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
            // requires a recent-ish MEncoder (from June 2010)
            set '-referrer': $URI
            $URI = 'http://www.eurogamer.net/' + browse { $('a.download').@href }
        }
    }
}
