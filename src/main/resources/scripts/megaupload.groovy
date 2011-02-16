// XXX this script requires PMSEncoder >= 1.4.0
// redirect Megaupload links to Megavideo

script {
    profile ('Megaupload') {
        pattern {
            domain 'megaupload.com'
        }

        action {
            $URI = browse (uri: $HTTP.target($URI)) { $('a.mvlink').@href }
        }
    }
}
