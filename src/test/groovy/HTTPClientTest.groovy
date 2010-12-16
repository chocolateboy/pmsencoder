@Typed
package com.chocolatey.pmsencoder

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import java.lang.NoClassDefFoundError

/*
    prior to Groovy++ 0.4.99, HTTPBuilder (via HTTPClient) was acting flaky under
    MIXED typing. try to pin it down
*/

class HTTPClientTest extends PMSEncoderTestCase {
    private HTTPClient http = new HTTPClient()

    void testHead() {
        assertFalse(http.head('http://www.example.com/nosuchfile.com'))
        assertTrue(http.head('http://www.example.com'))
    }

    void testGet() {
        assertNull(http.get('http://www.example.com/nosuchfile.com'))
        def example = http.get('http://www.example.com')
        assertNotNull(example)
        assertThat(example, instanceOf(String.class))
        assert example =~ 'RFC\\s+2606'
    }

    void testTarget() {
        assertNull(http.target('http://www.example.com/nosuchfile.com'))
        def target = http.target('http://www.sun.com')
        assertNotNull(target)
        assert target =~ '\\.oracle\\.com/'
    }
}
