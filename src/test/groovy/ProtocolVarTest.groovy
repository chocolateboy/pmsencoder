package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class ProtocolVarTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/protocol_var.groovy')
        matcher.load(script)
    }

    void testFileProtocol() {
        assertMatch([
            uri: 'file://some.file',
            wantMatches: [ 'file://' ],
            wantTranscoder: [ '-protocol', 'file' ]
        ])
    }

    void testHTTPProtocol() {
        assertMatch([
            uri: 'http://some.domain.com',
            wantMatches: [ 'http://' ],
            wantTranscoder: [ '-protocol', 'http' ]
        ])
    }

    void testSetProtocol() {
        assertMatch([
            uri: 'mms://www.example.com',
            wantMatches: [ 'mms://' ],
            wantURI: 'mmsh://www.example.com',
        ])
    }
}
