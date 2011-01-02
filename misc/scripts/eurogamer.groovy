// videofeed.Web,Test,WinFuture=http://rss.feedsportal.com/c/617/f/448481/index.rss
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
            $URI = 'http://www.eurogamer.net/' + browse { $('a.download', 0).@href }
        }
    }
}
