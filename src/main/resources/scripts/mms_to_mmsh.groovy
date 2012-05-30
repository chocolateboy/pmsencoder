check {
    profile ('mms to mmsh') {
        pattern {
            protocol 'mms'
            match { $TRANSCODER[0] == 'FFMPEG' }
        }

        action {
            $PROTOCOL = 'mmsh'
        }
    }
}
