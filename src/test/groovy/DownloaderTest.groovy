@Typed
package com.chocolatey.pmsencoder

class DownloaderTest extends PMSEncoderTestCase {
    def downloader = '/usr/bin/downloader'

    void testDownloaderString() {
        def uri = 'http://www.downloader-string.com'
        assertMatch([
            // groovy compiles scripts into a class with the script name e.g. downloader.class,
            // which wreaks havoc with downloader = ... (you tried to assign ... to a class),
            // so give the test a name that routes around this
            script: '/downloader_test.groovy',
            uri: uri,
            wantMatches: [ 'Downloader String' ],
            wantDownloader: [ downloader, 'string', uri ]
        ])
    }

    void testDownloaderList() {
        def uri = 'http://www.downloader-list.com'
        assertMatch([
            script: '/downloader_test.groovy',
            uri: uri,
            wantMatches: [ 'Downloader List' ],
            wantDownloader: [ downloader, 'list', uri ]
        ])
    }
}
