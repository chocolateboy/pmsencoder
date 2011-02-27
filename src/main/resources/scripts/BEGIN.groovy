begin {
    // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    IPAD_USER_AGENT = 'Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) ' +
        'AppleWebKit/531.21.10 (KHTML, like Gecko) ' +
        'Version/4.0.4 Mobile/7B334b Safari/531.21.10'

    // pull globals from PMS.conf i.e. translate PMS.conf settings into global PMSEncoder values
    GET_FLASH_VIDEOS = pmsConf['get-flash-videos.path']
    HLS_PLAYER = pmsConf['hls-player.path']
    NOTIFY_SEND = pmsConf['notify-send.path']
    PERL = pmsConf['perl.path']
    PPLIVE = pmsConf['pplive.path']
    PPLIVE_URI = pmsConf['pplive.uri'] ?: 'http://127.0.0.1:8888' // only used if PPLIVE is defined
    PYTHON = pmsConf['python.path']
    RTMPDUMP = pmsConf['rtmpdump.path']
    SOPCAST = pmsConf['sopcast.path']
    SOPCAST_URI = pmsConf['sopcast.uri'] ?: 'http://127.0.0.1:8902/stream' // only used if SOPCAST is defined
    VLC = pmsConf['vlc.path']
    // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    YOUTUBE_DL = pmsConf['youtube-dl.path']
    YOUTUBE_DL_MAX_QUALITY = pmsConf['youtube-dl.max-quality'] ?: 22
}
