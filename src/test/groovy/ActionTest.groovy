@Typed
package com.chocolatey.pmsencoder

class ActionTest extends PMSEncoderTestCase {
    void testScrapeURI() {
        def customConfig = this.getClass().getResource('/action.groovy')
        def uri = 'http://action.com'
        def command = new Command([ uri: uri ])
        def wantCommand = new Command(
            [ uri: uri, rfc: '2606' ], []
        )

        matcher.load(customConfig)

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Scrape' ],  // expected matches
        )
    }
}
