config {
    profile('Eq') {
        pattern {
            match { uri == 'http://foo.bar.baz' }
        }

        action {
            eq = uri
        }
    }

    profile ('Apple 3') {
        pattern {
            match { matches.containsAll([ 'Apple Trailers', 'Apple Trailers HD' ]) }
        }

        action {
            id = profile.name
        }
    }
}
