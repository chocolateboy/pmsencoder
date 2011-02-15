// videofeed.Web,Wimp=http://www.wimp.com/rss/

script {
    profile ('Get Flash Videos') {
        pattern {
            match { GET_FLASH_VIDEOS != null }
            domains([ 'wimp.com', 'megavideo.com' ]) // &c.
        }

        action {
            $DOWNLOADER = "$PERL $GET_FLASH_VIDEOS --quality high --quiet --yes --filename $DOWNLOADER_OUT \"${$URI}\""
        }
    }
}
