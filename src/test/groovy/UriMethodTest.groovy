@Typed
package com.chocolatey.pmsencoder

class UriMethodTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/uri_method.groovy')
        matcher.load(script)
    }

    void testFileProtocol() {
        assertMatch([
            uri: 'mms://www.example.com',
            wantMatches: [ 'uri_method' ],
            wantTranscoder: [ '-uri', 'mms://www.example.com', '-scheme', 'mms' ]
        ])
    }
}
