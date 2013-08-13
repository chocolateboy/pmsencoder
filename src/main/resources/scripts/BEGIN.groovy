script (BEGIN) {
    // pull globals from PMS.conf i.e. translate PMS.conf settings into global PMSEncoder values
    GET_FLASH_VIDEOS = pmsConf['get-flash-videos.path']
    NOTIFY_SEND = pmsConf['notify-send.path']
    PERL = pmsConf['perl.path']
    PYTHON = pmsConf['python.path']
    RTMPDUMP = pmsConf['rtmpdump.path']
    YOUTUBE_DL = pmsConf['youtube-dl.path']
    // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    YOUTUBE_DL_MAX_QUALITY = pmsConf['youtube-dl.max-quality'] ?: 22
}
