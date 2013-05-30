package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class StageTest extends PMSEncoderTestCase {
    void testStageOrdering() {
        assertMatch([
            script: '/stage.groovy',
            wantMatches: [
                'Begin 1',
                'Begin 2',
                'Init 1',
                'Init 2',
                'Script 1',
                'Script 2',
                'Check 1',
                'Check 2',
                'End 1',
                'End 2',
            ],
        ])
    }
}
