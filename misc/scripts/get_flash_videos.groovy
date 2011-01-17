// videofeed.Web,Wimp=http://www.wimp.com/rss/

script {
    // FIXME: these should be defined in (e.g.) settings.groovy
    def GET_FLASH_VIDEOS = '/usr/bin/get_flash_videos'
    def PERL = '/usr/bin/perl'

    profile ('Get Flash Videos') {
        pattern {
            domains([ 'wimp.com' ]) // &c.
        }

        action {
            $DOWNLOADER = "$PERL $GET_FLASH_VIDEOS --quality high --quiet --yes --filename $DOWNLOADER_OUT ${$URI}"
        }
    }
}
