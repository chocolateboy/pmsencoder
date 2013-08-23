package com.chocolatey.pmsencoder
/*
    prior to Groovy++ 0.4.99, HTTPBuilder (via HTTPClient) was acting flaky under
    MIXED typing. seems to be fixed now
*/

@groovy.transform.CompileStatic
class HTTPClientTest extends PMSEncoderTestCase {
    private HTTPClient http = new HTTPClient()

    void testHead() {
        assert http.head('http://www.example.com') == true
        assert http.head('http://ps3mediaserver.googlecode.com/nosuchfile.txt') == false
    }

    void testGet() {
        def example = http.get('http://www.example.com')

        assert example != null
        assert example instanceof String
        assert example =~ /\biana\b/
        assert http.get('http://ps3mediaserver.googlecode.com/nosuchfile.txt') == null
    }

    void testTarget() {
        def target = http.target('http://www.sun.com')

        assert target != null
        assert target =~ '\\.oracle\\.com/'
        assert http.target('http://ps3mediaserver.googlecode.com/nosuchfile.txt') == null
    }
}
