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
            uri: 'http://www.eurogamer.net/videos/uncharted-3-chateau-gameplay-part-2',
            matches: [ 'Browse' ],
            wantStash: [
                '$URI':
                'http://www.eurogamer.net/downloads/80345/uncharted-3-chateau-gameplay-part-2_stream_h264v2_large.mp4'
            ]
        ])
    }
}
