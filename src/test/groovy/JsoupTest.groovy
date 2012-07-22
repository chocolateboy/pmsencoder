@Typed
package com.chocolatey.pmsencoder

class JsoupTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/jsoup_test.groovy')
        matcher.load(script)
    }

    void testJsoup() {
        assertMatch([
            uri: 'http://www.ps3mediaserver.org',
            wantMatches: [ 'jsoup' ],
            wantStash: [
                $URI: 'http://www.ps3mediaserver.org',
                $title1: 'PS3 Media Server',
                $title2: 'PS3 Media Server',
                $title3: 'PS3 Media Server',
                $title4: 'PS3 Media Server'
            ]
        ])
    }
}
