@Typed
package com.chocolatey.pmsencoder

class PatternTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/pattern.groovy')
        matcher.load(script)
    }

    void testPatternMatchBlock() {
        def uri =  'http://foo.bar.baz'

        assertMatch([
            uri: uri,
            wantStash: [ $URI: uri, $eq: uri ],
            matches: [ 'Eq' ]
        ])
    }

    void testPatternMatchKeyString() {
        assertMatch([
            uri: 'http://key.string.com',
            matches: [ 'Key String' ]
        ])
    }

    void testPatternMatchKeyList() {
        assertMatch([
            uri: 'http://key.list.com',
            matches: [ 'Key List' ]
        ])
    }

    void testPatternMatchStringString() {
        assertMatch([
            uri: 'http://string.string.com',
            matches: [ 'String String' ]
        ])
    }

    void testPatternMatchStringList() {
        assertMatch([
            uri: 'http://string.list.com',
            matches: [ 'String List' ]
        ])
    }

    void assertMatch() {
        def uri = 'http://trailers.apple.com/movies/fox_searchlight/127hours/127hours-tlr1_h720p.mov'
        assertMatch([
            uri: uri,
            wantStash: [ $URI: uri, $profile: 'Apple 3' ],
            wantArgs: [ '-ofps', '24', '-user-agent', 'QuickTime/7.6.2' ],
            matches: [
                'Apple Trailers',
                'Apple Trailers HD',
                'Apple 3'
            ]
        ])
    }
}
