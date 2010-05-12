// add a new profile
config {
    profile ('Example') {
        match {
            matches uri: '^http://www\\.example\\.com\\b'
        }

        action {
            set an: 'example'
        }
    }
}
