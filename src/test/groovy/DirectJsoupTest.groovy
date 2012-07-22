@Typed
package com.chocolatey.pmsencoder

// test/exercise/experiment with things we may not be using in scripts yet

class DirectJsoupTest extends PMSEncoderTestCase {
    void testJsoupDirect() {
        def profileDelegate = getProfileDelegate()
        assert profileDelegate.$(uri: 'http://news.bbc.co.uk/')('title').text() == 'BBC News - Home'
        assert profileDelegate.jsoup(uri: 'http://www.ps3mediaserver.org/').title() == 'PS3 Media Server'
    }
}
