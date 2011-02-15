@Typed
package com.chocolatey.pmsencoder

class ProtocolVarTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/protocol_var.groovy')
        matcher.load(script)
    }

    void testFileProtocol() {
        assertMatch([
            uri: 'file://some.file',
            matches: [ 'file://' ],
            wantTranscoder: [ '-protocol', 'file' ]
        ])
    }

    void testHTTPProtocol() {
        assertMatch([
            uri: 'http://some.domain.com',
            matches: [ 'http://' ],
            wantTranscoder: [ '-protocol', 'http' ]
        ])
    }
}
