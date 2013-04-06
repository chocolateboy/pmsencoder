@Typed
package com.chocolatey.pmsencoder

class BrowseTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/browse.groovy')
        matcher.load(script)
    }

    void testBrowseReturn() {
        assertMatch([
            uri: 'http://www.ps3mediaserver.org',
            wantMatches: [ 'Browse' ],
            wantStash: [
                '$title': 'PS3 Media Server',
                '$URI':   'http://www.ps3mediaserver.org'
            ]
        ])
    }
}
