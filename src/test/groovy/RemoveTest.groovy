@Typed
package com.chocolatey.pmsencoder

class RemoveTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/remove.groovy')
        matcher.load(script)
    }

    void testRemoveName() {
        assertMatch([
            uri: 'http://remove.name',
            transcoder: [ '-foo', '-bar', '-baz', '-quux' ],
            wantMatches: [ 'Remove Name' ],
            wantTranscoder: [ '-foo', '-baz', '-quux' ]
        ])
    }

    void testRemoveValue() {
        assertMatch([
            uri: 'http://remove.value',
            transcoder: [ '-foo', '-bar', 'baz', '-quux' ],
            wantMatches: [ 'Remove Value' ],
            wantTranscoder: [ '-foo', '-quux' ]
        ])
    }

    void testDigitValue() {
        assertMatch([
            uri: 'http://digit.value',
            transcoder: [ '-foo', '-bar', '-42', '-quux' ],
            wantMatches: [ 'Digit Value' ],
            wantTranscoder: [ '-foo', '-quux' ]
        ])
    }

    void testHyphenValue() {
        assertMatch([
            uri: 'http://hyphen.value',
            transcoder: [ '-foo', '-output', '-', '-quux' ],
            wantMatches: [ 'Hyphen Value' ],
            wantTranscoder: [ '-foo', '-quux' ]
        ])
    }
}
