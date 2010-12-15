config {
    def downloader = '/usr/bin/downloader'

    profile ('Downloader List') {
        pattern {
            domain 'downloader-list.com'
        }

        action {
            $DOWNLOADER = [ downloader, 'list', $URI ]
        }
    }

    profile ('Downloader String') {
        pattern {
            domain 'downloader-string.com'
        }

        action {
            $DOWNLOADER = "$downloader string ${$URI}"
        }
    }
}
