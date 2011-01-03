/*
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/Latest30CinemaCreated/
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/Next30InCinema/
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/Latest30InCinema/
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/latest30ondvd/
*/

script {
    // sizes: small, medium, large, xlarge, xxlarge
    // def SIZE = 'xxlarge'

    profile ('Kino Trailers') {
        pattern {
            domain 'de.filmtrailer.com'
        }

        action {
            // as is usual with Flash, metadata is stored in an axternal file - in this
            // case XML
            def flashVarsUrl = browse { $('param', name: 'FlashVars').@value.minus(~/^file=/) }

            // use HTTPBuilder to convert the XML to a GPathResult object
            def flashVars = $HTTP.getXML(flashVarsUrl)

            // we can't use a variable (SIZE) here (yet) because a) the binding's are wrong and b)
            // XmlSlurper intercepts aggressively intercepts all property references
            def clips = flashVars.'**'.findAll { it.'@size' == 'xxlarge' }.collect { it.text() }

            // so far, the clips have all been 24 fps, so this isn't needed
            remove '-ofps'

            // in the absence of a custom transcoder, this is added last, so grab the last clip
            // in the list
            $URI = clips.pop()

            // and prepend (in order) the clips before it - if any
            clips.each { append([ it ]) }
        }
    }
}
