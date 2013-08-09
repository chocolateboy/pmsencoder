// videostream.Web,Screen=Screen,pmsencoder://dshow
// see: https://ffmpeg.org/trac/ffmpeg/wiki/How%20to%20grab%20the%20desktop%20%28screen%29%20with%20FFmpeg

// since this produces an invalid URI (i.e. video="UScreenCapture"),
// we need to run it as late as possible so that it doesn't break
// scripts that call uri()

import com.sun.jna.Platform

end {
    profile ('pmsencoder://dshow') {
        pattern {
            match { Platform.isWindows() }
            match uri: '^pmsencoder://dshow\\b'
        }

        action {
            uri = 'video="UScreenCapture"'
            set '-f': 'dshow'
        }
    }
}
