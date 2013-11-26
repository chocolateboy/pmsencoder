import static com.chocolatey.pmsencoder.FileUtil.*

script (BEGIN) {
    // pull globals from PMS.conf i.e. translate PMS.conf settings into global PMSEncoder values
    FFMPEG_LOG_LEVEL = 'warning' // i.e. -loglevel warning
    GET_FLASH_VIDEOS = pmsConf['get-flash-videos.path'] ?: which('get_flash_videos')
    NOTIFY_SEND = pmsConf['notify-send.path']
    PERL = pmsConf['perl.path'] ?: which('perl')
    PYTHON = pmsConf['python.path'] ?: which('python')
    RTMPDUMP = pmsConf['rtmpdump.path'] ?: which('rtmpdump')
    YOUTUBE_DL = pmsConf['youtube-dl.path'] ?: which('youtube-dl')
    // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    YOUTUBE_DL_MAX_QUALITY = pmsConf['youtube-dl.max-quality'] ?: 37 // (1080p)
    YOUTUBE_DL_PATH = getPath(PYTHON, YOUTUBE_DL)
    GET_FLASH_VIDEOS_PATH = getPath(PERL, GET_FLASH_VIDEOS)

    // log the youtube-dl version
    if (YOUTUBE_DL_PATH) {
        YOUTUBE_DL_VERSION = (YOUTUBE_DL_PATH + [ '--version' ]).execute().text.trim()
    }
}
