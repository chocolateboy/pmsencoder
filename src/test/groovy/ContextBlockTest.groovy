@Typed
package com.chocolatey.pmsencoder

class ContextBlockTest extends PMSEncoderTestCase {
    void testContextBlockSet() {
        def uri = 'http://context-block-set.com'
        assertMatch([
            script: '/context_block.groovy',
            uri: uri,
            matches: [ 'Context Block', 'Context Block Set' ],
            wantHook: [ 'hook', '-foo', '-bar', '-baz', '-quux' ],
            wantDownloader: { List<String> downloader ->
                downloader[-2] == '-user-agent' && downloader[-1] == 'PS3 Media Server'
            },
            wantTranscoder: { List<String> transcoder ->
                def threadsIndex = transcoder.findIndexOf { it == '-threads' }
                return transcoder[ threadsIndex + 1 ] == '42'
            },
            wantOutput: [ '-target', 'pal-dvd' ],
            useDefaultTranscoder: true
        ])
    }

    void testContextBlockRemove() {
        def uri = 'http://context-block-remove.com'
        assertMatch([
            script: '/context_block.groovy',
            uri: uri,
            matches: [ 'Context Block', 'Context Block Remove' ],
            wantHook: [ 'hook', '-foo', '-baz' ],
            wantDownloader: [ 'MPLAYER', '-prefer-ipv4', '-dumpstream' ],
            wantTranscoder: [ 'FFMPEG', '-threads', '3' ],
            wantOutput: [],
            useDefaultTranscoder: true
        ])
    }
}
