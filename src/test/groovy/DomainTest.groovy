@Typed
package com.chocolatey.pmsencoder

class DomainTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/domain.groovy')
        matcher.load(script)
    }

    void testDomainString() {
        assertMatch([
            uri: 'http://www.domain-string.com',
            matches: [ 'Domain String' ]
        ])
    }

    void testDomainList() {
        assertMatch([
            uri: 'http://www.domain-list.com',
            matches: [ 'Domain List' ]
        ])
    }

    void testDomainsString() {
        assertMatch([
            uri: 'http://www.domains-string.com',
            matches: [ 'Domains String' ]
        ])
    }

    void testDomainsList() {
        assertMatch([
            uri: 'http://www.domains-list.com',
            matches: [ 'Domains List' ]
        ])
    }

    void testGotDot() {
        assertMatch([
            uri: 'http://www.dot.com',
            matches: [ 'Got Dot' ]
        ])
    }

    void testNotDot() {
        assertMatch([
            uri: 'http://www.dotacom',
            matches: []
        ])
    }
}
