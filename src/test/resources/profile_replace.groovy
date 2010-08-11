// override the TED profile
config {
    profile ('TED') {
        pattern {
            match uri: '^http://feedproxy\\.google\\.com/~r/TEDTalks_video\\b'
        }

        action {
            set '-foo': 'bar'
            let uri: "$uri/foo/bar.baz"
        }
    }
}
