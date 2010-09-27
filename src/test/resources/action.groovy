config {
    profile('Scrape') {
        pattern {
            match { $URI == 'http://action.com' }
        }

        action {
            scrape 'RFC\\s+(?<rfc>\\d+)', [ uri: 'http://example.com' ]
        }
    }

    profile ('Stringify Values') {
        // verify that these values are all stringified
        pattern {
            match { $URI == 'http://stringify.values.com' }
        }

        action {
            set '-foo':  42
            set '-bar':  3.1415927
            set '-baz':  true
            set '-quux': null
        }
    }
}
