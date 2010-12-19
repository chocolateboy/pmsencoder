// add a new profile
script {
    profile ('Example') {
        pattern {
            match $URI: '^http://www\\.example\\.com\\b'
        }

        action {
            set '-an': 'example'
        }
    }
}
