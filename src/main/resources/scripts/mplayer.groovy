end {
    profile ('MPlayer') {
        pattern {
            match {
                // http://www.ffmpeg.org/ffmpeg-doc.html#SEC33
                $PROTOCOL && !($PROTOCOL in [
                    'concat',
                    'file',
                    'gopher',
                    'http',
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
            if (!$DOWNLOADER) {
                $DOWNLOADER = $MPLAYER
            }
        }
    }
}
