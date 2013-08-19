package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class EventTest extends PMSEncoderTestCase {
    void testDefaultEvent() {
        assertMatch([
            script: '/event.groovy',
            wantMatches: [ 'Default', 'Transcode' ],
        ])
    }

    void testTranscodeEvent() {
        assertMatch([
            event: Event.TRANSCODE,
            script: '/event.groovy',
            wantMatches: [ 'Default', 'Transcode' ],
        ])
    }

    void testFinalizeEvent() {
        assertMatch([
            event: Event.FINALIZE,
            script: '/event.groovy',
            wantMatches: [ 'Finalize' ],
        ])
    }
}
