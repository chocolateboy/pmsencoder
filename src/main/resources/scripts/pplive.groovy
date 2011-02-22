script {
    profile ('PPLive') {
        pattern {
            protocol 'synacast'
            match { PPLIVE }
        }

        action {
            def quotedURI = quoteURI($URI)
            $HOOK = "$PPLIVE ${quotedURI}"
            $URI = PPLIVE_URI ?: 'http://127.0.0.1:8888'
        }
    }
}
