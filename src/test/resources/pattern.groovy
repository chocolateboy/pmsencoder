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
}
