script {
    profile ('Example Hook') {
        pattern {
            match { NOTIFY_SEND != NULL }
        }

        action {
            $HOOK = [ NOTIFY_SEND, 'PMSEncoder', "playing ${$URI}" ]
        }
    }
}
