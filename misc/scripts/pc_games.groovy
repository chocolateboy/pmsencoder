script {
    profile ('PC Games') {
        pattern {
            domain 'pcgames.de'
        }

        action {
            $URI = $HTTP.target($URI)
            scrape '\\bcurrentposition\\s*=\\s*(?<currentPosition>\\d+)\\s*;'
            scrape "'(?<URI>http://\\w+\\.gamereport\\.de/videos/[^']+?/${currentPosition}/[^']+)'"
            // don't need this
            remove '-ofps'
            // see blip9tv.groovy
            set '-noskip'
            set '-mc': 0
        }
    }
}
