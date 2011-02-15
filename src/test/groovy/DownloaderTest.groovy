@Typed
package com.chocolatey.pmsencoder

class DownloaderTest extends PMSEncoderTestCase {
    def downloader = '/usr/bin/downloader'

    void testDownloaderString() {
        def uri = 'http://www.downloader-string.com'
        assertMatch([
            script: '/downloader.groovy',
            uri: uri,
            matches: [ 'Downloader String' ],
            wantDownloader: [ downloader, 'string', uri ]
        ])
    }

    void testDownloaderList() {
        def uri = 'http://www.downloader-list.com'
        assertMatch([
            script: '/downloader.groovy',
            uri: uri,
            matches: [ 'Downloader List' ],
            wantDownloader: [ downloader, 'list', uri ]
        ])
    }
}
