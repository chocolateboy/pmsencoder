script {
    profile ('Protocol String') {
        pattern {
            protocol 'file'
        }
    }

    profile ('Protocol List') {
        pattern {
            protocol([ 'mms', 'rtmp' ])
        }
    }
}
