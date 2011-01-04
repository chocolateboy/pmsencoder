/*
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/Latest30CinemaCreated/
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/Latest30InCinema/
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/latest30ondvd/
    videofeed.Web,FilmTrailer=http://de.rss.filmtrailer.com/default/Next30InCinema/
    videofeed.Web,Filmtrailers=http://uk.rss.filmtrailer.com/default/Latest30CinemaCreated/
    videofeed.Web,Filmtrailers=http://uk.rss.filmtrailer.com/default/Latest30InCinema/
    videofeed.Web,Filmtrailers=http://uk.rss.filmtrailer.com/default/Next30InCinema/
*/

// XXX this script requires PMSEncoder >= 1.4.0

script {
    // sizes: small, medium, large, xlarge, xxlarge
    // def SIZE = 'xxlarge'

    profile ('Kino Trailers') {
        pattern {
            domain 'filmtrailer.com'
        }

        action {
            // as is usual with Flash, metadata is stored in an external file - in this
            // case XML
            def flashVarsUrl = browse { $('param', name: 'FlashVars').@value.minus(~/^file=/) }

            // use HTTPBuilder to convert the XML to a GPathResult object
            def flashVars = $HTTP.getXML(flashVarsUrl)

            // we can't use a variable (SIZE) here (yet) because a) the bindings are wrong and b)
            // XmlSlurper aggressively intercepts all property references
            def clips = flashVars.'**'.findAll { it.'@size' == 'xxlarge' }.collect { it.text() }

            // so far, the clips have all been 24 fps, so this isn't needed
            remove '-ofps'

            /*
                when using the default transcoder (MEncoder), the URI is appended as the last argument.
                that works for one URI, but here there may be multiple "clips". the solution is to
                set the URI to the last clip and append any predecessors before it e.g.

                    mencoder -o output.tmp ... clip1 clip2 clip3
            */

            $URI = clips.pop() // e.g. clip3 -- appended to the argument list later

            // and append (in order) the clips before it - if any
            clips.each { append(it) } // e.g. clip1, clip2 -- append to the argument list now
        }
    }
}
