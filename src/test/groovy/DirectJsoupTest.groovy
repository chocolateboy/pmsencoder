package com.chocolatey.pmsencoder

// test/exercise/experiment with things we may not be using in scripts yet

@groovy.transform.CompileStatic
class DirectJsoupTest extends PMSEncoderTestCase {
    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    void testJsoupDirect() {
        def profileDelegate = getProfileDelegate()
        assert profileDelegate.$(uri: 'http://www.ps3mediaserver.org/')('title').text() == 'PS3 Media Server'
        assert profileDelegate.jsoup(uri: 'http://www.ps3mediaserver.org/').title() == 'PS3 Media Server'
    }
}
