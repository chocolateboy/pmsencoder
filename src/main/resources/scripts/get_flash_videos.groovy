// videofeed.Web,Wimp=http://www.wimp.com/rss/

// FIXME find a better example as wimp.com doesn't currently work
// CHECK: youtube-dl (DEFAULT) is the preferred downloader
// so this must be run after it

import static com.chocolatey.pmsencoder.FileUtil.isExecutable
import com.sun.jna.Platform

script (CHECK) {
    profile ('Get Flash Videos') {
        pattern {
            match { GET_FLASH_VIDEOS_PATH }
            protocols([ 'http', 'https' ])
            match { isGetFlashVideosCompatible(GET_FLASH_VIDEOS_PATH, uri) }
        }

        action {
            if (Platform.isWindows()) {
                // --quiet: make sure it doesn't write to stdout
                downloader = GET_FLASH_VIDEOS_PATH + [ '--quality', 'high', '--quiet', '--yes', '--filename', 'DOWNLOADER_OUT', 'URI' ]
            } else {
                downloader = GET_FLASH_VIDEOS_PATH + [ '--quality', 'high', '--yes', '--filename', 'DOWNLOADER_OUT', 'URI' ]
            }
        }
    }
}
