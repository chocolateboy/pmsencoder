// videofeed.Web,The Onion=http://feeds.theonion.com/onn/mrss

script {
    profile ('Onion News Network') {
        pattern {
            domain 'theonion.com'
        }

        action {
            uri = $('source[type=video/mp4]').attr('src')
        }
    }
}
