script {
    def NOTIFY_SEND = '/usr/bin/notify-send'

    profile ('Test Hook', replaces: 'Default') {
        action {
            $HOOK = [ NOTIFY_SEND, 'PMSEncoder', "playing ${$URI}" ]
        }
    }
}
