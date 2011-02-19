/*
    this is the default/builtin PMSEncoder script. PMSEncoder loads it from
    src/main/resources/scripts/INIT.groovy

    see:

       http://github.com/chocolateboy/pmsencoder/blob/plugin/src/main/resources/scripts/INIT.groovy

    XXX: Don't use this as a tutorial/documentation; see the wiki instead.
    XXX: The scripting framework/DSL is constantly changing, so don't rely on anything here.
*/

init {
    def nbcores = $PMS.getConfiguration().getNumberOfCpuCores()
    // have to be completely silent on Windows as stdout is sent to the transcoder
    def mplayerLogLevel = $PMS.get().isWindows() ? 'all=-1' : 'all=2'

    /*
        Matcher-level (global) lists of strings that provide provide useful default options
        for ffmpeg (downloader/transcoder), mplayer (downloader) and mencoder (downloader, transcoder)

        $DOWNLOADER = $MPLAYER:

            $DOWNLOADER = "mplayer -msglevel all=2 -prefer-ipv4 -quiet -dumpstream -dumpfile $DOWNLOADER_OUT ${$URI}"

        $TRANSCODER = $FFMPEG:

            $TRANSCODER = "ffmpeg -v 0 -y -threads nbcores -i ${$URI} -sameq -target pal-dvd $TRANSCODER_OUT"
            $TRANSCODER = "ffmpeg -v 0 -y -threads nbcores -i $DOWNLOADER_OUT -sameq -target pal-dvd $TRANSCODER_OUT"

        $TRANSCODER = $MENCODER:

            $TRANSCODER = "mencoder -mencoder -options -o $TRANSCODER_OUT ${$URI}"
            $TRANSCODER = "mencoder -mencoder -options -o $TRANSCODER_OUT $DOWNLOADER_OUT"

        By default, ffmpeg is used without a separate downloader.

        If one of these built-in downloaders/transcoders is used, then PMSEncoder adds the appropriate
        input/output options as shown above. Otherwise they must be set manually.

        Note: the uppercase executable names (e.g. FFMPEG) are used to signal to PMSEncoder.groovy that the
        configured path should be substituted.

        matcher-scoped (i.e. global): $FFMPEG, $FFMPEG_OUT, $MENCODER, and $MPLAYER are lists of strings,
        but, as seen below, can be set as strings, which are split on whitespace

        profile-scoped: $DOWNLOADER, $TRANSCODER, $OUTPUT and $HOOK are similar, but only have profile-scope
    */

    // default ffmpeg transcode command - all of these defaults can be (p)redefined in a userscript (e.g. BEGIN.groovy)
    // XXX: Groovy quirk: !$FFMPEG means $FFMPEG is not a) null or b) empty
    // all four of these values are initialized to empty lists, so we're relying on the "is nonempty"
    // meaning for these checks
    if (!$FFMPEG)
        $FFMPEG = "FFMPEG -v 0 -y -threads ${nbcores}"

    // default ffmpeg output options
    if (!$FFMPEG_OUT)
        $FFMPEG_OUT = '-sameq -target pal-dvd'

    // default mencoder transcode command
    if (!$MENCODER) {
        $MENCODER = [
            'MENCODER', // XXX add support for mencoder-mt
            '-msglevel', 'all=2',
            '-quiet',
            '-prefer-ipv4',
            '-oac', 'lavc',
            '-of', 'lavf',
            '-lavfopts', 'format=dvd',
            '-ovc', 'lavc',
            '-lavcopts', "vcodec=mpeg2video:vbitrate=4096:threads=${nbcores}:acodec=ac3:abitrate=128",
            '-ofps', '25',
            '-cache', '16384', // default cache size; default minimum percentage is 20%
            '-vf', 'harddup'
        ]
    }

    // default mplayer download command
    if (!$MPLAYER)
        $MPLAYER = "MPLAYER -msglevel ${mplayerLogLevel} -quiet -prefer-ipv4 -dumpstream"

    /*
        this is the default list of YouTube format/resolution IDs we should accept/select - in descending
        order of preference.

        it can be modified globally (in a script) to add/remove a format, or can be overridden on
        a per-video basis by supplying a new list to the youtube method (see below) e.g.

        exclude '1080p':

            youtube $YOUTUBE_ACCEPT - [ 37 ]

        add '2304p':

            youtube([ 38 ] + $YOUTUBE_ACCEPT)

        For the full list of formats, see: http://en.wikipedia.org/wiki/YouTube#Quality_and_codecs
    */

    if (!$YOUTUBE_ACCEPT) {
        $YOUTUBE_ACCEPT = [
            37,  // 1080p
            22,  // 720p
            35,  // 480p
            34,  // 360p
            18,  // Medium
            5    // 240p
        ]
    }
}
