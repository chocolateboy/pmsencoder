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
            // in the absence of a $BEFORE hook, use MEncoder, which
            // doesn't immediately fall over if a network resource is
            // unavailable. Thanks to SharkHunter for pointing this out
            // http://ps3mediaserver.org/forum/viewtopic.php?f=6&t=8776&view=unread#p46785
            $TRANSCODER = $MENCODER
        }
    }
}
