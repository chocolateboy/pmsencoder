package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class PatternTest extends PMSEncoderTestCase {
    void testStashAssignment() {
        assertMatch([
            script: '/pattern.groovy',
            wantMatches: [ 'Succeed 1', 'Succeed 2' ],
            wantStash: [ var3: 'value3', var4: 'value4' ]
        ])
    }
}
