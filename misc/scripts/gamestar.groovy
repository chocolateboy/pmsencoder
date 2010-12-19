script {
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
}
