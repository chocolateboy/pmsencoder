@Typed
package com.chocolatey.pmsencoder

class ActionTest extends PMSEncoderTestCase {
    void testScrapeURI() {
        def uri = 'http://action.com'
        assertMatch([
            script: '/action.groovy',
            uri:    uri,
            wantStash: [
                $URI: uri,
                $rfc: '2606'
            ],
            matches: [ 'Scrape' ]
        ])
    }

    void testStringifyValues() {
        assertMatch([
            script: '/action.groovy',
            uri:    'http://stringify.values.com',
            wantArgs: [
                '-foo',  '42',
                '-bar',  '3.1415927',
                '-baz',  'true',
                '-quux', 'null'
            ],
            matches: [ 'Stringify Values' ]
        ])
    }

    // this went missing at some stage - make sure it stays put
    void testSetString() {
        assertMatch([
            script:   '/action.groovy',
            uri:      'http://set.string.com',
            wantArgs: [ '-nocache' ],
            matches:  [ 'Set String' ]
        ])
    }
}
