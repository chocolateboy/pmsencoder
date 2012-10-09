@Typed
package com.chocolatey.pmsencoder

class PrependTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/prepend.groovy')
        matcher.load(script)
    }

    void testPrependObjectZero() {
        assertMatch([
            uri: 'http://prepend.object.zero',
            wantHook: [ 'one' ],
            wantDownloader: [ 'one' ],
            wantTranscoder: [ 'one' ],
            wantMatches: [ 'Prepend Object Zero' ]
        ])
    }

    void testPrependObjectOne() {
        assertMatch([
            uri: 'http://prepend.object.one',
            wantHook: [ 'one', 'two' ],
            wantDownloader: [ 'one', 'two' ],
            wantTranscoder: [ 'one', 'two' ],
            wantMatches: [ 'Prepend Object One' ]
        ])
    }

    void testPrependObjectTwo() {
        assertMatch([
            uri: 'http://prepend.object.two',
            wantHook: [ 'one', 'three', 'two' ],
            wantDownloader: [ 'one', 'three', 'two' ],
            wantTranscoder: [ 'one', 'three', 'two' ],
            wantMatches: [ 'Prepend Object Two' ]
        ])
    }

    void testPrependListZero() {
        assertMatch([
            uri: 'http://prepend.list.zero',
            wantHook: [ 'one', 'two' ],
            wantDownloader: [ 'one', 'two' ],
            wantTranscoder: [ 'one', 'two' ],
            wantMatches: [ 'Prepend List Zero' ]
        ])
    }

    void testPrependListOne() {
        assertMatch([
            uri: 'http://prepend.list.one',
            wantHook: [ 'one', 'two', 'three' ],
            wantDownloader: [ 'one', 'two', 'three' ],
            wantTranscoder: [ 'one', 'two', 'three' ],
            wantMatches: [ 'Prepend List One' ]
        ])
    }

    void testPrependListTwo() {
        assertMatch([
            uri: 'http://prepend.list.two',
            wantHook: [ 'one', 'three', 'four', 'two' ],
            wantDownloader: [ 'one', 'three', 'four', 'two' ],
            wantTranscoder: [ 'one', 'three', 'four', 'two' ],
            wantMatches: [ 'Prepend List Two' ]
        ])
    }
}
