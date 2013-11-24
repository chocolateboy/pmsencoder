// videofeed.Web,Comedy Central=http://extechops.net/full-episode-feeds/daily-show.rss
// videofeed.Web,Comedy Central=http://extechops.net/full-episode-feeds/colbert-report.rss

script (INIT) {
    def SEPARATOR = System.getProperty('line.separator')
    def FILENAME = 'pmsencoder_ffmpeg_concat_%d_%d.txt'
    def TEMP_FOLDER = pms.getConfiguration().getTempFolder()

    profile ('YouTube-DL Concat') {
        pattern {
            match { YOUTUBE_DL_PATH }
            domains([ 'thedailyshow.com', 'colbertnation.com' ])
        }

        action {
            // doesn't use cmd.exe, so no need to quote the URI
            def command = YOUTUBE_DL_PATH + [ '-g', uri ]
            def uris = command.execute().text.readLines(); logger.debug("URIs: ${uris.inspect()}")
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
