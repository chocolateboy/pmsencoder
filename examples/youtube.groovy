// TODO: implement profile extensions with calls to the original pattern/action via e.g. base()

// add &has_verified=1 to the YouTube page URI to bypass age verification
config {
    extend ('YouTube') { // replace the built-in profile
        // extract the resource's video_id from the URI of the standard YouTube page
        pattern {
            base()
        }

        action {
            let uri: '${uri}&has_verified=1' // append &has_verified=1
	    base()
        }
    }
}
