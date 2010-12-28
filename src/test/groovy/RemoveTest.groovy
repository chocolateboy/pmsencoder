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
            args: [ '-foo', '-bar', '-baz', '-quux' ],
            matches: [ 'Remove Name' ],
            wantArgs: [ '-foo', '-baz', '-quux' ]
        ])
    }

    void testRemoveValue() {
        assertMatch([
            uri: 'http://remove.value',
            args: [ '-foo', '-bar', 'baz', '-quux' ],
            matches: [ 'Remove Value' ],
            wantArgs: [ '-foo', '-quux' ]
        ])
    }

    void testDigitValue() {
        assertMatch([
            uri: 'http://digit.value',
            args: [ '-foo', '-bar', '-42', '-quux' ],
            matches: [ 'Digit Value' ],
            wantArgs: [ '-foo', '-quux' ]
        ])
    }

    void testHyphenValue() {
        assertMatch([
            uri: 'http://hyphen.value',
            args: [ '-foo', '-output', '-', '-quux' ],
            matches: [ 'Hyphen Value' ],
            wantArgs: [ '-foo', '-quux' ]
        ])
    }

}
