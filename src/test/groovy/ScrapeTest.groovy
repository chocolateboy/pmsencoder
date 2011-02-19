@Typed
package com.chocolatey.pmsencoder

class ScrapeTest extends PMSEncoderTestCase {
    void testScrapeString() {
        def uri = 'http://scrape.string'
        assertMatch([
            script: '/scrape.groovy',
            uri: uri,
            wantMatches: [ 'Scrape String' ],
            wantStash: [ $URI: uri, $first: 'scrape', $second: 'string' ]
        ])
    }
}
