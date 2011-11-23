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
            uri: 'http://www.example.org',
            wantMatches: [ 'Browse' ],
            wantStash: [
                '$title': 'IANA — Example domains',
                '$URI':   'http://www.example.org'
            ]
        ])
    }
}
