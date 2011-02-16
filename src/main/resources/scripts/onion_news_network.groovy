script {
    profile ('Onion News Network') {
        pattern {
            domain 'theonion.com'
        }

        action {
            // chase redirects (if possible)
            $URI = $HTTP.target($URI) ?: $URI
        }
    }
}
