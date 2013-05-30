// videofeed.Web,GiantBomb=http://www.giantbomb.com/feeds/video/
// see: http://www.giantbomb.com/feeds/

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
