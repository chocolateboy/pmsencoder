config {
    profile('Eq') {
        pattern {
            match { uri == 'http://foo.bar.baz' }
        }

        action {
            eq = uri
        }
    }
}
