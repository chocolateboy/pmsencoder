script {
    profile ('file://') {
        pattern {
            protocol 'file'
            protocol ([ 'file' ])
            protocol ([ 'file', 'file' ])
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
            protocol ([ 'http' ])
            protocol ([ 'http', 'http' ])
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
            protocol ([ 'mms' ])
            protocol ([ 'mms', 'mms' ])
            match { protocol == 'mms' }
            match { protocol != 'file' }
        }

        action {
            protocol = 'mmsh'
        }
    }
}
