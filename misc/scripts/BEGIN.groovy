// global variables available in all scripts; uncomment/adjust accordingly.
// XXX do *not* uncomment lines unless a) the section is for your platform and b)
// you actually have the specified application installed

begin {
    PPLIVE_URI = 'http://127.0.0.1:8888' // only used if PPLIVE is defined below
    SOPCAST_URI = 'http://127.0.0.1:8902/stream' // only used if SOPCAST_SERVER is defined below
    // see https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    YOUTUBE_DL_MAX_QUALITY = 22

    if ($PMS.isWindows()) {
        // GET_FLASH_VIDEOS = 'C:\\Perl\\Scripts\\get_flash_videos'
        // HLS_PLAYER = 'C:\\Python\\bin\\hls-player''
        // PERL = 'C:\\Perl\\bin\\perl'
        // PPLIVE = 'C:\\Program Files (x86)\\PPLive\\PPTV\\PPLive.exe'
        // PYTHON = 'C:\\Python\\bin\\python'
        // SOPCAST_SERVER = 'C:\\Path\\To\\SopCast.exe'
        // YOUTUBE_DL = 'C:\\Python\\Scripts\\youtube-dl'
    } else {
        // GET_FLASH_VIDEOS = '/usr/bin/get_flash_videos'
        // HLS_PLAYER = '/usr/bin/hls-player'
        // NOTIFY_SEND = '/usr/bin/notify-send'
        // PERL = '/usr/bin/perl'
        // PYTHON = '/usr/bin/python'
        // SOPCAST_SERVER = '/usr/bin/sopcast-server'
        // YOUTUBE_DL = '/usr/bin/youtube-dl'
    }
}
