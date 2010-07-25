@Typed
package com.chocolatey.pmsencoder

// import com.chocolatey.pmsencoder.RegexHelper

/*
    for some reason, HTTPBuilder (via HTTPClient) is acting flaky under
    DYNAMIC typing. try to pin it down
*/

class HTTPClientTest extends PMSEncoderTestCase {
    private HTTPClient http = new HTTPClient()

    void testHead() {
	assert http.head('http://www.example.com/nosuchfile.com') == false
	assert http.head('http://www.example.com/') == true
    }

    void testGet() {
	assert http.get('http://www.example.com/nosuchfile.com') == null
	def example = http.get('http://www.example.com')
	assert example != null
	assert example instanceof String
	assert example =~ 'RFC\\s+2606'
    }
}
