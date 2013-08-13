// videofeed.Web,Hardware Clips=http://www.hardwareclips.com/rss/new/
// videofeed.Web,Hardware Clips=http://www.hardwareclips.com/rss/views/
// videofeed.Web,Hardware Clips=http://www.hardwareclips.com/rss/comments/

script {
    profile ('HardwareClips') {
        pattern {
            domain 'hardwareclips.com'
        }

        action {
            // get the URI of the "JSON" file that contains the video stream URI
            def jsonUri = http.getNameValueMap($('link[rel=video_src]').attr('href')).config

            // the "JSON" is invalid (uses single quotes), so we can't use getJSON
            scrape uri: jsonUri, "'(?<uri>http://(\\w+\\.)?hardwareclips.com:8080/[^']+)'"
        }
    }
}
