script {
    profile ('Args List') {
        pattern {
            domain 'args.list'
        }

        action {
            $DOWNLOADER = [ 'list', $URI ]
        }
    }

    profile ('Args String') {
        pattern {
            domain 'args.string'
        }

        action {
            $DOWNLOADER = "string ${$URI}"
        }
    }
}
