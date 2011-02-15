script {
    profile ('SopCast') {
        pattern {
            protocol 'sop'
            match { SOPCAST != null }
        }

        action {
            $HOOK = "$SOPCAST \"${$URI}\""
            $URI = 'http://127.0.0.1:8902/stream'
        }
    }
}
