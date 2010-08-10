// add a new profile
config {
    profile ('Example') {
        pattern {
            match URI: '^http://www\\.example\\.com\\b'
        }

        action {
            set '-an': 'example'
        }
    }
}
