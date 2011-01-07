// videofeed.Web,Giantbomb=http://pipes.yahoo.com/pipes/pipe.run?_id=cf668f0d78945d30144d7d48d5021edc&_render=rss

script {
    profile ('GiantBomb') {
        pattern {
            domain 'giantbomb.com'
            scrape '"streaming_\\w+":\\s*"(?<extension>\\w+):(?<path>[^"]+)"'
        }

        action {
            $URI = "http://media.giantbomb.com/${path}.${extension}"
            // we're not changing the fps, so we don't need this
            remove '-ofps'
            // or this
            remove '-vf'
            // "rename" the framerate (29.97: not OK; 30000/1001: OK)
            set '-fps': '30000/1001'
        }
    }
}
