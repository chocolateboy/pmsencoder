check {
    profile ('mms to mmsh') {
        pattern {
            protocol 'mms'
            match { transcoder[0] == 'FFMPEG' }
        }

        action {
            protocol = 'mmsh'
        }
    }
}
