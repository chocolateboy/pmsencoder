package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class ExtensionMethodsTest extends PMSEncoderTestCase {
    void testPMSConf() {
        assertMatch([
            script: '/extension_methods.groovy',
            wantMatches: [ 'Extension Methods' ],
            wantTranscoder: [ '-indexed', 'oo', '-bool1', 'true', '-bool2', 'false' ]
        ])
    }
}
