script {
    profile ('SopCast') {
        pattern {
            match { SOPCAST_PLAYER && SOPCAST_URI }
            protocol 'sop'
        }

        action {
            $URI = quoteURI($URI)
            $HOOK = "$SOPCAST ${$URI}"
            $URI = SOPCAST_URI
        }
    }
}
