// override the TED profile
config {
    profile ('TED') {
        pattern {
            match $URI: '^http://feedproxy\\.google\\.com/~r/TEDTalks_video\\b'
        }

        action {
            set '-foo': 'bar'
            let $URI: "${$URI}/foo/bar.baz"
        }
    }
}
