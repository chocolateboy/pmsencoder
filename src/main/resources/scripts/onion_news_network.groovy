// videofeed.Web,The Onion=http://feeds.theonion.com/onn/mrss

script {
    profile ('Onion News Network') {
        pattern {
            domain 'theonion.com'
        }

        action {
            $URI = browse { $('source[type="video/mp4"]').@src }
        }
    }
}
