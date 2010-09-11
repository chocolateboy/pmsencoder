config {
    profile ('Politiek 24') {
        pattern {
            match { $URI == 'http://livestreams.omroep.nl/nos/politiek24-bb' }
        }

        action {
            // grab the .asx file and extract the first stream into $URI
            // FIXME - dollar in group name doesn't work with current (2010-09-07) version of RegExPlus
            scrape '<Ref\\s+href="(?<$URI>[^"]+)"\\s*/>'
        }
    }
}
