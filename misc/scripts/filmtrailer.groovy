/*
    videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/Latest30CinemaCreated/
    videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/Latest30InCinema/
    videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/latest30ondvd/
    videofeed.Web,Filmtrailer=http://de.rss.filmtrailer.com/default/Next30InCinema/
    videofeed.Web,Filmtrailer=http://fr.rss.filmtrailer.com/default/Latest30CinemaCreated/
    videofeed.Web,Filmtrailer=http://fr.rss.filmtrailer.com/default/Latest30InCinema/
    videofeed.Web,Filmtrailer=http://fr.rss.filmtrailer.com/default/Next30InCinema/
    videofeed.Web,Filmtrailer=http://uk.rss.filmtrailer.com/default/Latest30CinemaCreated/
    videofeed.Web,Filmtrailer=http://uk.rss.filmtrailer.com/default/Latest30InCinema/
    videofeed.Web,Filmtrailer=http://uk.rss.filmtrailer.com/default/Next30InCinema/
*/

// XXX this script requires PMSEncoder >= 1.4.0

// FIXME: these should be converted into ATOM feeds with multiple enclosures (clips)
// XXX need to improve PMS's feed handling

script {
    def SIZES = [ 'xxlarge', 'xlarge', 'large', 'medium', 'small' ]

    profile ('Filemtrailer.com') {
        pattern {
            domain 'filmtrailer.com'
        }

        action {
            // as is usual with Flash, metadata is stored in an external file - in this
            // case XML
            def flashVarsUrl = browse { $('param', name: 'FlashVars').@value.minus(~/^file=/) }

            // use HTTPBuilder to convert the XML to a GPathResult object
            def flashVars = $HTTP.getXML(flashVarsUrl)

            // assemble a list of clip URIs; typically each trailer has several
            // variants: shorter, longer, teaser, excerpt, international, interviews &c.
            def clips

            /*
                example trailer that doesn't have an xxlarge clip:

                    page: http://uk.filmtrailer.com/trailer/5644/127+hours+film+t
                    xml:  http://uk.player-feed.previewnetworks.com/v3.1/cinema/5644/441100000-1/
            */

            SIZES.any { size ->
                log.info("looking for clip size: $size")

                clips = flashVars.'**'.findAll { it.'@size' == size }.collect { it.text() }

                if (clips.size() > 0) {
                    log.info('success')
                    return true
                } else {
                    log.info('failure')
                    return false
                }
            }

            // now use the ffmpeg "concat" pseudo-protocol to join the clips together
            $URI = 'concat:' + clips.join('\\|')
        }
    }
}
