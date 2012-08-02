script {
    def downloaderPath = '/usr/bin/downloader'

    profile ('Downloader List') {
        pattern {
            domain 'downloader-list.com'
        }

        action {
            downloader = [ downloaderPath, 'list', uri ]
        }
    }

    profile ('Downloader String') {
        pattern {
            domain 'downloader-string.com'
        }

        action {
            downloader = "$downloaderPath string ${uri}"
        }
    }
}
