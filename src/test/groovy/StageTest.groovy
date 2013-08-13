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
                'Default 1',
                'Default 2',
                'Default 3',
                'Default 4',
                'Check 1',
                'Check 2',
                'End 1',
                'End 2',
            ],
        ])
    }
}
