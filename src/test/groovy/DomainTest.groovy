@Typed
package com.chocolatey.pmsencoder

class DomainTest extends PMSEncoderTestCase {
    private URL customConfig = this.getClass().getResource('/domain.groovy')

    void testDomainString() {
        def uri = 'http://www.domain-string.com'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'Domain String' ]
    }

    void testDomainList() {
        def uri = 'http://www.domain-list.com'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'Domain List' ]
    }

    void testDomainsString() {
        def uri = 'http://www.domains-string.com'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'Domains String' ]
    }

    void testDomainsList() {
        def uri = 'http://www.domains-list.com'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'Domains List' ]
    }

    void testGotDot() {
        def uri = 'http://www.dot.com'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == [ 'Got Dot' ]
    }

    void testNotDot() {
        def uri = 'http://www.dotacom'
        def command = new Command([ $URI: uri ])

        matcher.load(customConfig)
        // bypass Groovy's annoyingly loose definition of true
        assertSame(false, matcher.match(command, false)) // false: don't use default transcoder args

        assert command.matches == []
    }
}
