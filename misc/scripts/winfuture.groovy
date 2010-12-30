// videofeed.Web,Test,WinFuture=http://rss.feedsportal.com/c/617/f/448481/index.rss
script {
    profile ('Redirect') {
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
            // http://lists.mplayerhq.hu/pipermail/mplayer-dev-eng/2010-December/067084.html
            $URI = $HTTP.target(URLDecoder.decode($escaped))
        }
    }
}
