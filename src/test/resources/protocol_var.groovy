script {
    profile ('file://') {
        pattern {
            protocol 'file'
            match { $PROTOCOL == 'file' }
            match { $PROTOCOL != 'http' }
        }

        action {
            set '-protocol': $PROTOCOL

        }
    }

    profile ('http://') {
        pattern {
            protocol 'http'
            match { $PROTOCOL == 'http' }
            match { $PROTOCOL != 'file' }
        }

        action {
            set '-protocol': $PROTOCOL
        }
    }
}
