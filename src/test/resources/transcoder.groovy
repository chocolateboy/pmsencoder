script {
    def transcoderPath = '/usr/bin/transcoder'

    profile ('Transcoder List') {
        pattern {
            domain 'transcoder-list.com'
        }

        action {
            transcoder = [ transcoderPath, 'list', uri ]
        }
    }

    profile ('Transcoder String') {
        pattern {
            domain 'transcoder-string.com'
        }

        action {
            transcoder = "$transcoderPath string ${uri}"
        }
    }
}
