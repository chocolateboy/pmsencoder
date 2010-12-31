script {
    profile ('SopCast') {
        pattern {
            protocol 'sop'
        }

        action {
            $HOOK = "sopcast ${$URI}"
            $URI = 'http://127.0.0.1:8902/stream'
        }
    }
}
