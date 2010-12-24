@Typed
package com.chocolatey.pmsencoder

class ActionTest extends PMSEncoderTestCase {
    void testScrapeURI() {
        def script = this.getClass().getResource('/action.groovy')
        def uri = 'http://action.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri, $rfc: '2606' ], []
        )

        matcher.load(script)

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Scrape' ],  // expected matches
        )
    }

    void testStringifyValues() {
        def script = this.getClass().getResource('/action.groovy')
        def uri = 'http://stringify.values.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri ],
            [
                '-foo',  '42',
                '-bar',  '3.1415927',
                '-baz',  'true',
                '-quux', 'null'
            ]
        )

        matcher.load(script)

        assertMatch(
            command,                 // supplied command
            wantCommand,             // expected command
            [ 'Stringify Values' ],  // expected matches
        )
    }

    // this went missing at some stage - make sure it stays put
    void testSetString() {
        def script = this.getClass().getResource('/action.groovy')
        def uri = 'http://set.string.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri ],
            [ '-nocache' ]
        )

        matcher.load(script)

        assertMatch(
            command,           // supplied command
            wantCommand,       // expected command
            [ 'Set String' ],  // expected matches
        )
    }

}
