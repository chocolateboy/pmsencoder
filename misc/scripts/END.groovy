script {
    profile ('Unsupported FFmpeg Protocol') {
        pattern {
            match {
                // http://www.ffmpeg.org/ffmpeg-doc.html#SEC33
                !($PROTOCOL in [
                    'file',
                    'gopher',
                    'http',
                    'pipe',
                    'rtmp',
                    'rtmpt',
                    'rtmpe',
                    'rtmpte',
                    'rtmps',
                    'rtp',
                    'tcp',
                    'udp',
                    'concat'
                ])
            }
        }

        action {
            if ($DOWNLOADER == null) {
                $DOWNLOADER = $MPLAYER
            }
        }
    }
}
