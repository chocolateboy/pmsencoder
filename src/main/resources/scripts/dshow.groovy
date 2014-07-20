// videostream.Web,Screen=Screen,pmsencoder://dshow
// see: https://ffmpeg.org/trac/ffmpeg/wiki/How%20to%20grab%20the%20desktop%20%28screen%29%20with%20FFmpeg

import com.sun.jna.Platform

script {
    profile ('pmsencoder://dshow') {
        pattern {
            match { Platform.isWindows() }
            match { uri == 'pmsencoder://dshow' }
        }

        action {
            uri = 'video="UScreenCapture"'
            set '-f': 'dshow'
        }
    }
}
