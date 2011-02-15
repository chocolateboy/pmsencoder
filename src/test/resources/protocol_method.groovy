script {
    profile ('Protocol String') {
        pattern {
            protocol 'dvb'
        }
    }

    profile ('Protocol List') {
        pattern {
            protocol([ 'mms', 'sop' ])
        }
    }
}
