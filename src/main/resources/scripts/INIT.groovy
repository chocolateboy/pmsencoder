/*
    this is the default/builtin PMSEncoder script. PMSEncoder loads it from
    src/main/resources/scripts/INIT.groovy

    see:

       http://github.com/chocolateboy/pmsencoder/blob/plugin/src/main/resources/scripts/INIT.groovy

    XXX: Don't use this as a tutorial/documentation; see the wiki instead.
    XXX: The scripting framework/DSL is constantly changing, so don't rely on anything here.
*/

script (INIT) {
    def nbcores = pms.getConfiguration().getNumberOfCpuCores()

    /*
        Matcher-level (global) list of strings that define the ffmpeg command:

            transcoder = "FFMPEG -loglevel warning -y -threads nbcores \
                -i ${uri} -output -args TRANSCODER_OUT"

            transcoder = "FFMPEG -loglevel warning -y -threads nbcores \
                -i DOWNLOADER_OUT -output -args TRANSCODER_OUT"

        By default, ffmpeg is used without a separate downloader.

        If ffmpeg is used, PMSEncoder adds the appropriate output options as shown above. Otherwise they must be set manually.

        Note: the uppercase executable name (e.g. FFMPEG) is used to signal to PMSEncoder.groovy that the
        configured ffmpeg path should be substituted.

        matcher-scoped (i.e. global): FFMPEG is a list of strings, but, as seen below, can be assigned a string,
        which is split on whitespace.

        profile-scoped: downloader, transcoder and hook are similar, but are only defined in the context
        of a profile block.
    */

    // default ffmpeg input options - all of these defaults can be (p)redefined in a userscript (e.g. BEGIN.groovy)
    if (!FFMPEG) // not null and not empty
        FFMPEG = "FFMPEG -loglevel ${FFMPEG_LOG_LEVEL} -y -threads ${nbcores}" // -threads 0 doesn't work for all codecs - better to specify

    /*
        this is the default list of YouTube format/resolution IDs we should accept/select - in descending
        order of preference.

        it can be modified globally (in a script) to add/remove a format, or can be overridden on
        a per-video basis by supplying a new list to the youtube method (see below) e.g.

        exclude '1080p':

            youtube YOUTUBE_ACCEPT - [ 37 ]

        add '3072p':

            youtube([ 38 ] + YOUTUBE_ACCEPT)

        For the full list of formats, see: http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    */

    if (!YOUTUBE_ACCEPT) {
        YOUTUBE_ACCEPT = [
            37,  // 1080p XXX no longer available
            22,  // 720x1280 mp4
            35,  // 480p XXX no longer available
            34,  // 360p XXX no longer available
            18,  // 360x640 mp4
            43,  // 360x640 webm
            5    // 240x400 flv
        ]
    }
}
