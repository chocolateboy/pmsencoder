script {
   profile ('Gameswelt') {
        pattern {
            match $URI: '^http://www\\.gameswelt\\.de/videos/videos/'
        }

        action {
            // extract the video URI from the value of the flashvars param
            scrape '<param\\s+name="flashvars".+(?<URI>http://video\\.gameswelt\\.de/[^&]+)'
        }
    }
}
