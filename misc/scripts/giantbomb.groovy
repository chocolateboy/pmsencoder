// videofeed.Web,GiantBomb=http://pipes.yahoo.com/pipes/pipe.run?_id=cf668f0d78945d30144d7d48d5021edc&_render=rss

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
