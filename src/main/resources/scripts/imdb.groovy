check {
    profile ('IMDb Trailer') {
        pattern {
            domain 'imdb.com'
            match uri: '/title/(?<imdb_id>tt\\d+)'
        }

        action {
            def domain = uri().host
            def trailers = $(uri: "http://${domain}/title/${imdb_id}/videogallery/content_type-Trailer?sort=6")
            def viconst = trailers('img.video[viconst]').attr('viconst') // get the ID of the first trailer
            if (viconst) {
                scrape(uri: "http://${domain}/video/screenplay/${viconst}/player?uff=3")(
                    '\\bIMDbPlayer\\.playerKey\\s*=\\s*"(?<uri>[^"]+)"\\s*;'
                )
            }
        }
    }
}
