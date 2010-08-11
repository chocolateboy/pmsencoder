// add a new profile
config {
    profile ('Example') {
        pattern {
            match uri: '^http://www\\.example\\.com\\b'
        }

        action {
            set '-an': 'example'
        }
    }
}
