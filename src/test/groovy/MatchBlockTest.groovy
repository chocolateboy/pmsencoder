@Typed
package com.chocolatey.pmsencoder

class PatternTest extends PMSEncoderTestCase {
    void testPatternEq() {
        def customConfig = this.getClass().getResource('/match_block.groovy')
        def uri = 'http://foo.bar.baz'
        def command = new Command([ uri: uri ])
        def wantCommand = new Command(
            [ uri: uri, eq: uri ], []
        )

        matcher.load(customConfig)

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Eq' ],      // expected matches
        )
    }
}
