script {
    def NOTIFY_SEND = '/usr/bin/notify-send'

    profile ('Test Hook', before: 'BEGIN') {
        action {
            $HOOK = [ NOTIFY_SEND, 'PMSEncoder', "playing ${$URI}" ]
        }
    }
}
