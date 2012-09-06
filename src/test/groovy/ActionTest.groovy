@Typed
package com.chocolatey.pmsencoder

class ActionTest extends PMSEncoderTestCase {
    void testIsOption() {
        def action = getAction()

        assert action.isOption('-foo')
        assert action.isOption('-Foo')
        assert action.isOption('-FOO')

        assert action.isOption('-foo-bar')
        assert action.isOption('-Foo-Bar')
        assert action.isOption('-FOO-BAR')

        assert action.isOption('--foo')
        assert action.isOption('--Foo')
        assert action.isOption('--FOO')

        assert action.isOption('--foo-bar')
        assert action.isOption('--Foo-Bar')
        assert action.isOption('--FOO-BAR')
    }

    void testIsNotOption() {
        def action = getAction()

        assert !action.isOption(null)
        assert !action.isOption('')
        assert !action.isOption('-')
        assert !action.isOption('--')
        assert !action.isOption('---')
        assert !action.isOption('---foo')
        assert !action.isOption('---Foo')
        assert !action.isOption('---FOO')
        assert !action.isOption('foo')
        assert !action.isOption('foo-bar')
        assert !action.isOption('Foo')
        assert !action.isOption('Foo-Bar')
        assert !action.isOption('FOO')
        assert !action.isOption('FOO-BAR')
        assert !action.isOption('-42')
        assert !action.isOption('-3.1415927')
        assert !action.isOption('-foo:')
        assert !action.isOption('--foo:')
        assert !action.isOption('-f!o')
        assert !action.isOption('--f!o')
        assert !action.isOption('-~foo')
        assert !action.isOption('--~foo')
        assert !action.isOption('-=')
        assert !action.isOption('--=')
    }

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
