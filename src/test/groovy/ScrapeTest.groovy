@Typed
package com.chocolatey.pmsencoder

class ScrapeTest extends PMSEncoderTestCase {
    void testScrapeString() {
        def uri = 'http://scrape.string'
        assertMatch([
            script: '/scrape.groovy',
            uri: uri,
            wantMatches: [ 'Scrape String' ],
            wantStash:  [
                uri:    'http://scrape.string',
                first:  'scrape',
                second: 'string',
                third:  'scrape',
                fourth: 'string',
                fith:   'scrape',
                sixth:  'string'
            ]
        ])
    }
}
