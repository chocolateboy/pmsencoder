config {
    profile('Scrape') {
        pattern {
            match { uri == 'http://action.com' }
        }

        action {
            scrape 'RFC\\s+(?<rfc>\\d+)', [ uri: 'http://example.com' ]
        }
    }
}
