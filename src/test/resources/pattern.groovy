config {
    profile('Eq') {
        pattern {
            match { $URI == 'http://foo.bar.baz' }
        }

        action {
            eq = $URI
        }
    }

    profile ('Apple 3', after: [ 'Apple Trailers', 'Apple Trailers HD' ]) {
        pattern {
            // test match List<String>
            match([ 'Apple Trailers', 'Apple Trailers HD' ])
        }

        action {
            profile = 'Apple 3'
        }
    }

    profile ('Key String') {
        pattern {
            match '$URI': 'http://key.string.com'
        }
    }

    profile ('Key List') {
        pattern {
            match '$URI': [ 'http://www.nosuchdomain.com', 'http://key.list.com' ]
        }
    }

    profile ('String String') {
        pattern {
            def $uri = $URI
            match $uri, 'http://string.string.com'
        }
    }

    profile ('String List') {
        pattern {
            def $uri = $URI
            match $uri, [ 'http://string.list.com' ]
        }
    }
}
