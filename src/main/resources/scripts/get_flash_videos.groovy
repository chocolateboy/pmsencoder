// videofeed.Web,Wimp=http://www.wimp.com/rss/

script {
    profile ('Get Flash Videos') {
        def GET_FLASH_VIDEOS_PATH

        if (GET_FLASH_VIDEOS) {
            if ((new File(GET_FLASH_VIDEOS)).canExecute()) {
                GET_FLASH_VIDEOS_PATH = GET_FLASH_VIDEOS
            } else if (PERL) {
                GET_FLASH_VIDEOS_PATH = "$PERL $GET_FLASH_VIDEOS"
            }
        }

        pattern {
            match { GET_FLASH_VIDEOS_PATH }
            domains([ 'wimp.com' ]) // &c.
        }

        action {
            $URI = quoteURI($URI)
            def quiet = $PMS.isWindows() ? ' --quiet ' : ''
            $DOWNLOADER = "$GET_FLASH_VIDEOS_PATH --quality high ${quiet} --yes --filename $DOWNLOADER_OUT ${$URI}"
        }
    }
}
