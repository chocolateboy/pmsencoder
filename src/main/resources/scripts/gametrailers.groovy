// videofeed.Web,Test,GameTrailers=http://www.gametrailers.com/rssgenerate.php?s1=&favplats[ps3]=ps3&quality[hd]=on&agegate[no]=on&orderby=newest&limit=100
script {
    profile ('GameTrailers (Revert PMS Workaround)') {
        /*
           convert:

               http://www.gametrailers.com/download/48298/t_ufc09u_educate_int_gt.flv

           to:

               http://www.gametrailers.com/player/48298.html
         */

        // 1) extract the page ID
        pattern {
            match $URI: '^http://(www\\.)?gametrailers\\.com/download/(?<gametrailers_page_id>\\d+)/[^.]+\\.flv$'
        }

        // 2) and use it to restore the correct web page URI
        action {
           $URI = "http://www.gametrailers.com/player/${gametrailers_page_id}.html"
        }
    }

    profile ('GameTrailers') {
        pattern {
            domain 'gametrailers.com'
        }

        action {
            def gmi = scrape('\\bmov_game_id\\s*=\\s*(?<gametrailers_movie_id>\\d+)')
            def gfl = scrape('\\bhttp://www\\.gametrailers\\.com/download/\\d+/(?<gametrailers_filename>t_[^.]+)\\.wmv\\b')

            if (gmi && gfl) {
                $URI = "http://trailers-ak.gametrailers.com/gt_vault/${gametrailers_movie_id}/${gametrailers_filename}.flv"
            } else if (scrape('\\bvar\\s+mov_id\\s*=\\s*(?<gametrailers_mov_id>\\d+)')) {
                def scrapeURI = "http://www.gametrailers.com/neo/?page=xml.mediaplayer.Mediagen&movieId=${gametrailers_mov_id}&hd=1"
                scrape '<src>\\s*(?<URI>\\S+)\\s*</src>', [ uri: scrapeURI ]
            }
        }
    }
}
