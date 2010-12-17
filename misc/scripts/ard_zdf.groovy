config {
    profile ('ARD/ZDF') {
        pattern {
            match $URI: '^http://hstreaming\\.(ard|zdf)\\.de/.+?\\.mov$'
        }

        action {
            // scrape the RTSP URI from the .mov "container" (actually a plain text file)
            scrape '(?<URI>rtsp://\\S+)'
            // now set the correct MEncoder options
            set '-rtsp-stream-over-tcp' // needed for some firewalls/routers
        }
    }
}
