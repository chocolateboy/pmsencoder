begin {
    profile ('Notify Send', stopOnMatch: false) {
        pattern {
            match { NOTIFY_SEND }
        }

        action {
            hook = [ NOTIFY_SEND, 'PMSEncoder', "playing ${uri}" ]
        }
    }
}
