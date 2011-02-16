begin {
    // global variables available in all scripts; uncomment/adjust accordingly
    // a builtin version of this file with paths uncommented is loaded automatically
    // so simply copy that and set the values that should be defined or overidden

    // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    YOUTUBE_DL_MAX_QUALITY = 22
    IPAD_USER_AGENT = 'Mozilla/5.0 (iPad; U; CPU OS 3_2 like Mac OS X; en-us) ' +
        'AppleWebKit/531.21.10 (KHTML, like Gecko) ' +
        'Version/4.0.4 Mobile/7B334b Safari/531.21.10'
    SOPCAST_URI = 'http://127.0.0.1:8902/stream' // only used if SOPCAST is defined below

    if ($PMS.get().isWindows()) {
        // GET_FLASH_VIDEOS = 'C:\\Perl\\Scripts\\get_flash_videos'
        // HLS_PLAYER = 'C:\\Python\\bin\\hls-player''
        // PERL = 'C:\\Perl\\bin\\perl'
        // PYTHON = 'C:\\Python\\bin\\python'
        // SOPCAST = 'C:\\Path\\To\\SopCast.exe'
        // YOUTUBE_DL = 'C:\\Python\\Scripts\\youtube-dl'
    } else {
        // CVLC = '/usr/bin/cvlc'
        // GET_FLASH_VIDEOS = '/usr/bin/get_flash_videos'
        // HLS_PLAYER = '/usr/bin/hls-player'
        // NOTIFY_SEND = '/usr/bin/notify-send'
        // PERL = '/usr/bin/perl'
        // PYTHON = '/usr/bin/python'
        // SOPCAST = '/path/to/sopcast'
        // YOUTUBE_DL = '/usr/bin/youtube-dl'
    }
}
