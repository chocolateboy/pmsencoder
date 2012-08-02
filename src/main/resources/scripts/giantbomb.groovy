// videofeed.Web,OCD,GiantBomb=http://pipes.yahoo.com/pipes/pipe.run?_id=0a51536967fd09f9e85f37bddb6bb91d&_render=rss

script {
    profile ('GiantBomb') {
        pattern {
            domain 'giantbomb.com'
            scrape '&quot;streaming_\\w+&quot;:\\s*&quot;(?<extension>\\w+):(?<path>.+?)&quot;'
        }

        action {
            uri = "http://media.giantbomb.com/${path}.${extension}"
        }
    }
}
