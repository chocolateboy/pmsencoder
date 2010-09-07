config {
    profile('Scrape') {
        pattern {
            match { $URI == 'http://action.com' }
        }

        action {
            scrape 'RFC\\s+(?<rfc>\\d+)', [ uri: 'http://example.com' ]
        }
    }
}
