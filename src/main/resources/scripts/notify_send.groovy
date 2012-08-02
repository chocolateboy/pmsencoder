script {
    profile ('Notify Send') {
        pattern {
            match { NOTIFY_SEND }
        }

        action {
            hook = [ NOTIFY_SEND, 'PMSEncoder', "playing ${uri}" ]
        }
    }
}
