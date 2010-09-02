config {
    profile ('Politiek 24') {
        pattern {
            match uri: '^http://livestreams\\.omroep\\.nl/nos/politiek24-bb$'
        }

        action {
            // grab the .asx file and extract the first stream into $uri
            scrape '<Ref\\s+href="(?<uri>[^"]+)"\\s*/>'
        }
    }
}
