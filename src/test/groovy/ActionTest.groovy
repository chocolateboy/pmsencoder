@Typed
package com.chocolatey.pmsencoder

class ActionTest extends PMSEncoderTestCase {
    void testScrapeURI() {
        def uri = 'http://action.com'
        assertMatch([
            script: '/action.groovy',
            uri:    uri,
            wantStash: [
                uri: uri,
                rfc: '2606'
            ],
            wantMatches: [ 'Scrape' ]
        ])
    }

    void testStringifyValues() {
        assertMatch([
            script: '/action.groovy',
            uri:    'http://stringify.values',
            wantTranscoder: [
                '-foo',  '42',
                '-bar',  '3.1415927',
                '-baz',  'true',
                '-quux'
            ],
            wantMatches: [ 'Stringify Values' ]
        ])
    }

    // this went missing at some stage - make sure it stays put
    void testSetString() {
        assertMatch([
            script:   '/action.groovy',
            uri:      'http://set.string',
            wantTranscoder: [ '-nocache' ],
            wantMatches:  [ 'Set String' ]
        ])
    }

    void testSetMap() {
        assertMatch([
            script: '/action.groovy',
            uri:    'http://set.map',
            wantTranscoder: [
                '-foo',  '42',
                '-bar',  '3.1415927',
                '-baz',  'true',
                '-quux'
            ],
            wantMatches: [ 'Set Map' ]
        ])
    }
}
