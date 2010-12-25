@Typed
package com.chocolatey.pmsencoder

class TranscoderTest extends PMSEncoderTestCase {
    def transcoder = '/usr/bin/transcoder'

    void testTranscoderList() {
        def uri = 'http://www.transcoder-list.com'
        assertMatch([
            script: '/transcoder.groovy',
            uri: uri,
            matches: [ 'Transcoder List' ],
            transcoder: [ transcoder, 'list', uri ]
        ])
    }

    void testTranscoderString() {
        def uri = 'http://www.transcoder-string.com'
        assertMatch([
            script: '/transcoder.groovy',
            uri: uri,
            matches: [ 'Transcoder String' ],
            transcoder: [ transcoder, 'string', uri ]
        ])
    }
}
