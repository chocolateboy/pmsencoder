script {
    profile ('PPLive') {
        pattern {
            protocol 'synacast'
            match { PPLIVE }
        }

        action {
            def quotedURI = quoteURI(uri)
            hook = "$PPLIVE ${quotedURI}"
            uri = PPLIVE_URI ?: 'http://127.0.0.1:8888'
            // see sopcast.groovy
            transcoder = MENCODER
        }
    }
}
