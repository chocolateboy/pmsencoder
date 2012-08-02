script {
    profile ('file://') {
        pattern {
            protocol 'file'
            match { protocol == 'file' }
            match { protocol != 'http' }
        }

        action {
            set '-protocol': protocol

        }
    }

    profile ('http://') {
        pattern {
            protocol 'http'
            match { protocol == 'http' }
            match { protocol != 'file' }
        }

        action {
            set '-protocol': protocol
        }
    }

    profile ('mms://') {
        pattern {
            protocol 'mms'
            match { protocol == 'mms' }
            match { protocol != 'file' }
        }

        action {
            protocol = 'mmsh'
        }
    }
}
