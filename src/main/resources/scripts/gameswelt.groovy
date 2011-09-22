// videofeed.Web,Gameswelt=http://www.gameswelt.de/feeds/videos/rss.xml
script {
   profile ('Gameswelt') {
        pattern {
            match $URI: '^http://www\\.gameswelt\\.de/videos/'
        }

        // source: javascript: openDownloadsPopup('43769', '201109/insert_coin_sendung_81_teaser_gameswelt_tv_720p.mp4')
        // target: http://video.gameswelt.de/public/mp4/201109/43769_insert_coin_sendung_81_teaser_gameswelt_tv_720p.mp4
        // source: openDownloadsPopup('43514', '201109/Star_Wars_The_Complete_Saga_83053.mpg')
        // target: http://video.gameswelt.de/public/mp4/201109/43514_star_wars_the_complete_saga_83053.mp4
        action {
            scrape "\\bopenDownloadsPopup\\('(?<prefix>\\d+)',\\s*'(?<date>\\d+)/(?<path>[^.]+)"
            $URI = "http://video.gameswelt.de/public/mp4/${date}/${prefix}_${path.toLowerCase()}.mp4"
        }
    }
}
