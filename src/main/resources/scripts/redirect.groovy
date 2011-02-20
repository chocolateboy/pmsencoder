// work around incomplete HTTP support (for e.g. HTTP 301) in ffmpeg and older mplayer builds
init {
    profile ('Chase Redirects') {
        pattern {
            domain 'feedproxy.google.com'
        }

        action {
            $URI = $HTTP.target($URI)
        }
    }
}
