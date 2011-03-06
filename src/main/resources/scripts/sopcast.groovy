// videostream.Web,SopCast=SopCast,sop://sop.sopcast.org:1234/5678
script {
    profile ('SopCast') {
        pattern {
            match { SOPCAST }
            protocol 'sop'
        }

        action {
            def quotedURI = quoteURI($URI)
            $HOOK = [ SOPCAST, quotedURI ]
            $URI = SOPCAST_URI ?: 'http://127.0.0.1:8902/stream'
            // in the absence of a $BEFORE hook, try using MEncoder, which
            // may handle a missing network connection more gracefully (i.e. with retries)
            // Thanks to SharkHunter for the suggestion:
            // http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776&view=unread#p46785
            $TRANSCODER = $MENCODER
        }
    }
}
