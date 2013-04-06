// videofeed.Web,Test,GameTrailers=http://www.gametrailers.com/rssgenerate.php?s1=&favplats[ps3]=ps3&quality[hd]=on&agegate[no]=on&orderby=newest&limit=100

script {
    profile ('GameTrailers') {
        pattern { domain 'gametrailers.com' }

        action {
            def mov_game_id = scrape '\\bvar\\s+mov_game_id\\s*=\\s*(?<gametrailers_mov_game_id>\\d+)'
            def mov_id = scrape '\\bvar\\s+mov_id\\s*=\\s*(?<gametrailers_mov_id>\\d+)'
            def filename = scrape '/download/\\d+/(?<gametrailers_filename>t_[^.]+)\\.(mov|wmv|mp4)\\b'

            if (mov_game_id && filename) {
                uri = "http://download.gametrailers.com/gt_vault/${gametrailers_mov_game_id}/${gametrailers_filename}.flv"
            } else if (mov_id) {
                def scrapeURI = "http://www.gametrailers.com/neo/?page=xml.mediaplayer.Mediagen&movieId=${gametrailers_mov_id}&hd=1"
                scrape(uri: scrapeURI)('<src>\\s*(?<uri>\\S+)\\s*</src>')
            }
        }
    }
}
