package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class TranscoderTest extends PMSEncoderTestCase {
    def transcoder = '/usr/bin/transcoder'

    void testTranscoderList() {
        def uri = 'http://www.transcoder-list.com'
        assertMatch([
            script: '/transcoder.groovy',
            uri: uri,
            wantMatches: [ 'Transcoder List' ],
            transcoder: [ transcoder, 'list', uri ]
        ])
    }

    void testTranscoderString() {
        def uri = 'http://www.transcoder-string.com'
        assertMatch([
            script: '/transcoder.groovy',
            uri: uri,
            wantMatches: [ 'Transcoder String' ],
            transcoder: [ transcoder, 'string', uri ]
        ])
    }
}
