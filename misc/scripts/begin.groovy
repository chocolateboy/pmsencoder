script {
    // allow profiles to match the target URI rather than e.g.
    // http://feeds.example.com?redirect=http://target.uri/video/1234
    profile ('Chase Redirects', before: 'BEGIN') { // or 'replaces', or overwite BEGIN
        pattern {
            protocol([ 'http', 'https' ])
        }

        action {
            $URI = $HTTP.target($URI)
        }
    }
}
