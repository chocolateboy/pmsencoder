// videostream.Web,TV=Bloomberg TV,http://www.bloomberg.com/streams/video/LiveBTV200.asx

script {
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
}
