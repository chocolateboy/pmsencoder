// videofeed.Web,Wimp=http://www.wimp.com/rss/

script {
    // FIXME: these should be defined as global variables in (e.g.) INIT.groovy
    def GET_FLASH_VIDEOS = '/usr/bin/get_flash_videos'
    def PERL = '/usr/bin/perl'

    profile ('Get Flash Videos') {
        pattern {
            domains([ 'wimp.com', 'megavideo.com' ]) // &c.
        }

        action {
            $DOWNLOADER = "$PERL $GET_FLASH_VIDEOS --quality high --quiet --yes --filename $DOWNLOADER_OUT \"${$URI}\""
        }
    }
}
