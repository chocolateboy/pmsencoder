// videofeed.Web,Wimp=http://www.wimp.com/rss/

// FIXME find a better example as wimp.com doesn't currently work
// check: youtube-dl (stage: script) is the preferred downloader
// so this must be run later
check {
    profile ('Get Flash Videos') {
        def GET_FLASH_VIDEOS_PATH

        if (GET_FLASH_VIDEOS) {
            if ((new File(GET_FLASH_VIDEOS)).canExecute()) {
                GET_FLASH_VIDEOS_PATH = [ GET_FLASH_VIDEOS ]
            } else if (PERL) {
                GET_FLASH_VIDEOS_PATH = [ PERL, GET_FLASH_VIDEOS ]
            }
        }

        pattern {
            match { GET_FLASH_VIDEOS_PATH }
            protocols([ 'http', 'https' ])
            match { isGetFlashVideosCompatible(GET_FLASH_VIDEOS_PATH, uri) }
        }

        action {
            if (pms.isWindows()) {
                // --quiet: make sure it doesn't write to stdout
                downloader = GET_FLASH_VIDEOS_PATH + [ '--quality', 'high', '--quiet', '--yes', '--filename', 'DOWNLOADER_OUT', uri ]
            } else {
                downloader = GET_FLASH_VIDEOS_PATH + [ '--quality', 'high', '--yes', '--filename', 'DOWNLOADER_OUT', uri ]
            }
        }
    }
}
