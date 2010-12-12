config {
    def ICHC = 'I Can Has Cheezburger'
    def IPAD_USER_AGENT = 'Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) ' +
        'AppleWebKit/531.21.10 (KHTML, like Gecko) ' +
        'Version/4.0.4 Mobile/7B334b Safari/531.21.10'

    profile (ICHC) {
        pattern {
            match $URI: '^http://(\\w+\\.)?icanhascheezburger.com/'
        }
    }

    profile ('I Can Has YouTube', before: 'YouTube', after: ICHC) {
        pattern {
            match ICHC // string or List<String> in $MATCHES
            scrape '\\b(?<URI>http://www\\.youtube\\.com/v/[^&]+)'
        }
    }

    profile ('I Can Has Viddler', after: ICHC) {
        pattern {
            match ICHC // string or List<String> in $MATCHES
            scrape "\\bsrc='(?<URI>http://www\\.viddler\\.com/file/\\w+/html5mobile/)'"
        }

        action {
            set '-user-agent': IPAD_USER_AGENT
        }
    }
}
