script (BEGIN) {
    // pull globals from PMS.conf i.e. translate PMS.conf settings into global PMSEncoder values
    FFMPEG_LOG_LEVEL = 'warning' // i.e. -loglevel warning
    GET_FLASH_VIDEOS = pmsConf['get-flash-videos.path']
    NOTIFY_SEND = pmsConf['notify-send.path']
    PERL = pmsConf['perl.path']
    PYTHON = pmsConf['python.path']
    RTMPDUMP = pmsConf['rtmpdump.path']
    YOUTUBE_DL = pmsConf['youtube-dl.path']
    // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    YOUTUBE_DL_MAX_QUALITY = pmsConf['youtube-dl.max-quality'] ?: 37 // (1080p)

    if (YOUTUBE_DL) {
        if ((new File(YOUTUBE_DL)).canExecute()) {
            YOUTUBE_DL_PATH = [ YOUTUBE_DL ]
        } else if (PYTHON) {
            YOUTUBE_DL_PATH = [ PYTHON, YOUTUBE_DL ]
        }
    }
}
