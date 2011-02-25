// videofeed.Web,OCD,GiantBomb=http://pipes.yahoo.com/pipes/pipe.run?_id=0a51536967fd09f9e85f37bddb6bb91d&_render=rss

script {
    profile ('GiantBomb') {
        pattern {
            domain 'giantbomb.com'
            scrape '"streaming_\\w+":\\s*"(?<extension>\\w+):(?<path>[^"]+)"'
        }

        action {
            $URI = "http://media.giantbomb.com/${path}.${extension}"
        }
    }
}
