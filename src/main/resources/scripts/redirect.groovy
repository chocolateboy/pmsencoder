// work around incomplete HTTP support (for e.g. HTTP 301) in ffmpeg and older mplayer builds
init {
    profile ('Chase Redirects') { // if possible
        pattern {
            domain([ 'rss.feedsportal.com', 'feedproxy.google.com', 'theonion.com' ])
        }

        action {
            $URI = $HTTP.target($URI) ?: $URI
        }
    }
}
