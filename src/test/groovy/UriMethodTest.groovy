@Typed
package com.chocolatey.pmsencoder

class UriMethodTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/uri_method.groovy')
        matcher.load(script)
    }

    void testFileProtocol() {
        assertMatch([
            uri: 'mms://www.example.com',
            wantMatches: [ 'uri_method' ],
            wantTranscoder: [ '-uri', 'mms://www.example.com', '-scheme', 'mms' ]
        ])
    }

    void testUri() {
        def uri1 = 'http://www.example.com:12/foo?bar=baz'
        def uri2 = 'https://www.example.org:34/foobar?baz=quux'
        def command = new Command(new Stash([ $URI: uri1 ]))
        def profileDelegate = getProfileDelegate(command)

        assert profileDelegate.uri().getClass() == URI
        assert profileDelegate.uri(uri2).getClass() == URI

        assert profileDelegate.uri().toString() == uri1
        assert profileDelegate.uri(uri2).toString() == uri2

        assert profileDelegate.uri().scheme == 'http'
        assert profileDelegate.uri(uri2).scheme == 'https'

        assert profileDelegate.uri().host == 'www.example.com'
        assert profileDelegate.uri(uri2).host == 'www.example.org'

        assert profileDelegate.uri().port == 12
        assert profileDelegate.uri(uri2).port == 34

        assert profileDelegate.uri().path == '/foo'
        assert profileDelegate.uri(uri2).path == '/foobar'

        assert profileDelegate.uri().query == 'bar=baz'
        assert profileDelegate.uri(uri2).query == 'baz=quux'
    }
}
