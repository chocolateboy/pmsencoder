package com.chocolatey.pmsencoder

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

// test/exercise/experiment with things we may not be using in scripts yet

@CompileStatic
class DirectJsoupTest extends PMSEncoderTestCase {
    // CompileStatic error: Cannot find matching method java.lang.Object#text()
    @CompileStatic(TypeCheckingMode.SKIP)
    void testJsoupDirect() {
        def profileDelegate = getProfileDelegate()
        assert profileDelegate.$(uri: 'http://www.ps3mediaserver.org/')('title').text() == 'PS3 Media Server'
        assert profileDelegate.jsoup(uri: 'http://www.ps3mediaserver.org/').title() == 'PS3 Media Server'
    }
}
