script {
    profile ('Politiek 24') {
        pattern {
            match { $URI == 'http://livestreams.omroep.nl/nos/politiek24-bb' }
        }

        action {
            // grab the .asx file and extract the first stream into $URI
            scrape '<Ref\\s+href="(?<URI>[^"]+)"\\s*/>'
        }
    }
}
