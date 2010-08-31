config {
    profile('Eq') {
        pattern {
            eq uri: 'http://foo.bar.baz'
        }

        action {
            eq = uri
        }
    }
}
