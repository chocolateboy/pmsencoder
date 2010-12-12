config {
    macro ('MultiEmbed') { String $profile ->
        def IPAD_USER_AGENT = 'Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) ' +
            'AppleWebKit/531.21.10 (KHTML, like Gecko) ' +
            'Version/4.0.4 Mobile/7B334b Safari/531.21.10'

        profile ('Embed YouTube', before: 'YouTube', after: $profile) {
            pattern {
                match $profile // string or List<String> in $MATCHES
                scrape '\\b(?<URI>http://www\\.youtube\\.com/v/[^&]+)'
            }
        }

        profile ('Embed Viddler', after: $profile) {
            pattern {
                match $profile // string or List<String> in $MATCHES
                scrape "\\bsrc='(?<URI>http://www\\.viddler\\.com/file/\\w+/html5mobile/)'"
            }

            action {
                set '-user-agent': IPAD_USER_AGENT
            }
        }
    }

    profile ('I Can Has Cheezburger') {
        pattern {
            match $URI: '^http://(\\w+\\.)?icanhascheezburger.com/'
        }
    }

    apply ('MultiEmbed', 'I Can Has Cheezburger')
}
