script {
    def NOTIFY_SEND = '/usr/bin/notify-send'

    profile ('Example Hook') {
        action {
            $HOOK = [ NOTIFY_SEND, 'PMSEncoder', "playing ${$URI}" ]
        }
    }
}
