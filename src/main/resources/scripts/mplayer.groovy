end {
    profile ('MPlayer') {
        pattern {
            match { !downloader }
            // don't clobber MEncoder options if they've already been set
            match { transcoder[0] != 'MENCODER' }
            match {
                // MEncoder/MPlayer protocols that aren't supported by ffmpeg
                // Also see: http://www.ffmpeg.org/ffmpeg-doc.html#SEC33
                protocol && (protocol in [
                    'br',
                    'cdda',
                    'cddb',
                    'cue',
                    'dvb',
                    'dvd',
                    'dvdnav',
                    'ftp',
                    'http_proxy',
                    'icyx',
                    'mf',
                    'mmsu',
                    'mpst',
                    'noicyx',
                    'pvr',
                    'radio',
                    'rtsp',
                    'screen',
                    'sdp',
                    'smb',
                    'sop',
                    'synacast',
                    'tivo',
                    'tv',
                    'unsv',
                    'vcd'
                ])
            }
        }

        action {
            transcoder = MENCODER
        }
    }
}
