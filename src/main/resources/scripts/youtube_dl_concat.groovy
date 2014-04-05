import groovyx.net.http.URIBuilder

// videofeed.Web,Comedy Central=http://extechops.net/full-episode-feeds/daily-show.rss
// videofeed.Web,Comedy Central=http://extechops.net/full-episode-feeds/colbert-report.rss

script (INIT) {
    def SEPARATOR = System.getProperty('line.separator')
    def FILENAME = 'pmsencoder_ffmpeg_concat_%d_%d.txt'
    def TEMP_FOLDER = pms.getConfiguration().getTempFolder()

    // XXX not sure if this is needed for the previously-used domains (without the .cc)
    // or even if they're still used
    profile ('Comedy Central: /episodes/ -> /full-episodes/', stopOnMatch: false) {
        pattern {
            // this is a workaround for a youtube-dl bug (or at least a missing feature)
            match { YOUTUBE_DL_PATH }
            domains([ 'thedailyshow.cc.com', 'thecolbertreport.cc.com' ])
        }

        /*
         * XXX youtube-dl doesn't grok Comedy Central pages whose paths start with /episodes, but does handle
         * those that start with /full-episodes, so replace the former with the latter:
         *
         *     from: http://thedailyshow.com/episodes/wqf1x5/april-2--2014---samuel-l--jackson
         *     to:   http://thedailyshow.com/full-episodes/wqf1x5/april-2--2014---samuel-l--jackson
         */
        action {
            /*
             * XXX this would be much clearer/simpler if path was (also) exposed as a List e.g.:
             *
             *     def u = new URIBuilder(uri)
             *
             *     if (u.steps[0] == 'episodes') {
             *         u.steps[0] = 'full-episodes'
             *         uri = u.toString()
             *     }
             */
            def u = new URIBuilder(uri)
            def steps = u.path.split('/')

            // path starts with a forward slash (and split preserves it), so steps is e.g.:
            // [ '', 'episodes', 'wqf1x5', 'april-2--2014---samuel-l--jackson' ]
            if (steps[1] == 'episodes') {
                steps[1] = 'full-episodes'
                u.path = steps.join('/')
                uri = u.toString()
            }
        }
    }

    profile ('YouTube-DL Concat') {
        pattern {
            match { YOUTUBE_DL_PATH }
            domains([ 'thedailyshow.com', 'thedailyshow.cc.com', 'colbertnation.com', 'thecolbertreport.cc.com' ])
        }

        action {
            // this command doesn't use cmd.exe, so no need to quote the URI
            def command = YOUTUBE_DL_PATH + [ '-g', uri ]
            def uris = command.execute().text.readLines()

            logger.debug("URIs: ${uris.inspect()}")

            def files = uris.collect({ "file '$it'" }).join(SEPARATOR)
            def filename = String.format(FILENAME, Thread.currentThread().getId(), System.currentTimeMillis())
            def file = new File(TEMP_FOLDER, filename); logger.trace("concat file: ${file.getAbsolutePath()}")

            file.write(files)
            file.deleteOnExit()
            set '-f': 'concat'
            uri = file.getAbsolutePath()
        }
    }
}
