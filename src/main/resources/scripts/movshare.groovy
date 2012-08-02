script {
    profile ('MovShare') {
        pattern {
            domain 'movshare.net'
        }

        // if vlc is defined, use that as the downloader (starts instantly for some reason)
        // otherwise wait for the delay
        action {
            if (VLC) {
                uri = quoteURI(uri)
                downloader = "$VLC -q -I dummy --demux dump --demuxdump-file $DOWNLOADER_OUT ${uri}"
            } else {
                params.waitbeforestart = 20000L
            }
        }
    }
}
