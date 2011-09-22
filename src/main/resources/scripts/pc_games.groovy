// videofeed.Web,PCGames=http://videos.pcgames.de/rss

script {
    profile ('PC Games') {
        pattern {
            domain 'pcgames.de'
        }

        // source: flashvars='config=http://videos.pcgames.de/embed/pcgames/7aaa99f9804d0905fe58/' />
        // source: 'url': 'http://videos.pcgames.de:8080/405a31b06b9ece50772e9c4d9f04826d/4899_mini.mp4',
        // target: http://videos.pcgames.de:8080/405a31b06b9ece50772e9c4d9f04826d/4899_mini.mp4
        action {
            $URI = $HTTP.target($URI)
            scrape "\\bflashvars='config=(?<URI>[^']+)'"
            scrape "'url':\\s*'(?<URI>http://videos.pcgames.de:8080/[^']+)'"
        }
    }
}
