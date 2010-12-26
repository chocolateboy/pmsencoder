@Typed
package com.chocolatey.pmsencoder

class RemoveTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/remove.groovy')
        matcher.load(script)
    }

    void testSmartRemoveName() {
        assertMatch([
            uri: 'http://smart.remove.name',
            args: [ '-foo', '-bar', '-baz', '-quux' ],
            matches: [ 'Smart Remove Name' ],
            wantArgs: [ '-foo', '-baz', '-quux' ]
        ])
    }

    void testSmartRemoveValue() {
        assertMatch([
            uri: 'http://smart.remove.value',
            args: [ '-foo', '-bar', 'baz', '-quux' ],
            matches: [ 'Smart Remove Value' ],
            wantArgs: [ '-foo', '-quux' ]
        ])
    }

    void testRemoveNWithOption() {
        assertMatch([
            uri: 'http://remove.n',
            args: [ '-foo', '-bar', '-baz', '-quux' ],
            matches: [ 'Remove N' ],
            wantArgs: [ '-quux' ]
        ])
    }

    void testRemoveNWithoutOption() {
        assertMatch([
            uri: 'http://remove.n',
            args: [ '-foo', '-bar', 'baz', '-quux' ],
            matches: [ 'Remove N' ],
            wantArgs: [ '-quux' ]
        ])
    }
}
