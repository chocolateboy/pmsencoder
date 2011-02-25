@Typed
package com.chocolatey.pmsencoder

class PMSConfTest extends PMSEncoderTestCase {
    void testPMSConf() {
        assertMatch([
            script: '/pmsconf.groovy',
            wantMatches: [ 'pmsConf' ],
            wantTranscoder: [ '-rtmpdump-path', '/usr/bin/rtmpdump' ]
        ])
    }
}
