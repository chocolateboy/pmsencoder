// videofeed.Web=http://day9tv.blip.tv/rss

/*
    with the default options (and all but one suggested workaround), MEncoder dies with a
    "dimensions not set" / "Floating point exception" error. This is a known issue:

        http://bugzilla.mplayerhq.hu/show_bug.cgi?id=1337
        http://www.mailinglistarchive.com/html/mplayer-dev-eng@mplayerhq.hu/2010-12/msg00152.html

    -noskip fix cargo-culted from here:

        http://www.mailinglistarchive.com/mplayer-users@mplayerhq.hu/msg09774.html

    ffmpeg probably works as well (with e.g. MPlayer as the downloader), but this is easier
*/

script {
    profile ('blip9tv MP4') {
        pattern {
            match $URI: '^http://blip\\.tv/file/get/Striderdoom-.+?\\.m(p4|4v)$'
        }

        action {
            // we're not changing the fps, so we don't need this
            remove '-ofps'
            // "rename" the framerate (29.97: not OK; 30000/1001: OK)
            set '-fps': '30000/1001'
            // cargo-culted fix: see above
            set '-noskip'
            // and while we're at it, may as well include this (from below)
            set '-mc': 2
        }
    }

    profile ('blip9tv Legacy') {
        pattern {
            match $URI: '^http://blip\\.tv/file/get/Striderdoom-.+?\\.(flv|avi)$'
        }

        action {
            // we're not changing the fps, so we don't need this
            remove '-ofps'
            // they're all either exactly 30 fps, or e.g. 30.003 fps, so round down so they're MPEG-2 compliant
            set '-fps': 30
            // even with the correct framerate, it's still a struggle to keep the A/V in sync
            set '-noskip'
            set '-mc': 2
            // the video is a fraction of a second ahead of the audio in these, for some reason (even in MPlayer)
            set '-delay': 0.5
        }
    }
}
