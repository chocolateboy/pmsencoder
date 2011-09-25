end {
    profile ('MPlayer') {
        pattern {
            match { !$DOWNLOADER }
            match {
                // http://www.ffmpeg.org/ffmpeg-doc.html#SEC33
                $PROTOCOL && !($PROTOCOL in [
                    'concat',
                    'file',
                    'gopher',
                    'http',
                    'https',
                    'pipe',
                    'rtmp',
                    'rtmpe',
                    'rtmps',
                    'rtmpt',
                    'rtmpte',
                    'rtp',
                    'tcp',
                    'udp'
                ])
            }
        }

        action {
            // don't clobber MEncoder options if they've already been set
            if (!($TRANSCODER[0] == 'MENCODER' || $TRANSCODER[0] == 'MENCODER_MT'))
                $TRANSCODER = $MENCODER
        }
    }
}
