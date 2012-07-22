script {
    profile('Scrape') {
        pattern {
            match { $URI == 'http://action.com' }
        }

        action {
            scrape uri: 'http://example.com', 'RFC\\s+(?<rfc>\\d+)'
        }
    }

    profile ('Stringify Values') {
        // verify that these values are all stringified
        pattern {
            domain 'stringify.values'
        }

        action {
            set '-foo':  42
            set '-bar':  3.1415927
            set '-baz':  true
            set '-quux': null // but not this
        }
    }

    profile ('Set String') {
        pattern {
            domain 'set.string'
        }

        action {
            set '-nocache'
        }
    }

    profile ('Set Map') {
        pattern {
            domain 'set.map'
        }

        action {
            set '-foo':  42, '-bar':  3.1415927, '-baz': true, '-quux': null
        }
    }
}
