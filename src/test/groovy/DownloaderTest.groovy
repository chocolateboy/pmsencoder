@Typed
package com.chocolatey.pmsencoder

class DownloaderTest extends PMSEncoderTestCase {
    def downloader = '/usr/bin/downloader'

    void testDownloaderList() {
        def customConfig = this.getClass().getResource('/downloader.groovy')
        def uri = 'http://www.downloader-list.com'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'Downloader List' ]
        assert command.downloader == [ downloader, 'list', uri ]
    }

    void testDownloaderString() {
        def customConfig = this.getClass().getResource('/downloader.groovy')
        def uri = 'http://www.downloader-string.com'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'Downloader String' ]
        assert command.downloader == [ downloader, 'string', uri ]
    }
}
