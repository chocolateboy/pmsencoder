script {
    def transcoder = '/usr/bin/transcoder'

    profile ('Transcoder List') {
        pattern {
            domain 'transcoder-list.com'
        }

        action {
            $TRANSCODER = [ transcoder, 'list', $URI ]
        }
    }

    profile ('Transcoder String') {
        pattern {
            domain 'transcoder-string.com'
        }

        action {
            $TRANSCODER = "$transcoder string ${$URI}"
        }
    }
}
