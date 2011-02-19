@Typed
package com.chocolatey.pmsencoder

class ContextBlockTest extends PMSEncoderTestCase {
    void testContextBlockSet() {
        def uri = 'http://context-block-set.com'
        assertMatch([
            script: '/context_block.groovy',
            uri: uri,
            wantMatches: [ 'Context Block', 'Context Block Set' ],
            wantHook: [ 'hook', '-foo', '-bar', '-baz', '-quux' ],
            downloader: [ 'downloader', '-foo' ],
            wantDownloader: [ 'downloader', '-foo', '-bar' ],
            transcoder: [ 'transcoder', '-foo' ],
            wantTranscoder: [ 'transcoder', '-foo', '-bar', '-baz' ],
            output: [ '-output', '-foo' ],
            wantOutput: [ '-output', '-foo', '-bar' ]
        ])
    }

    void testContextBlockRemove() {
        def uri = 'http://context-block-remove.com'
        assertMatch([
            script: '/context_block.groovy',
            uri: uri,
            wantMatches: [ 'Context Block', 'Context Block Remove' ],
            wantHook: [ 'hook', '-foo', '-baz' ],
            downloader: [ 'downloader', '-foo', '-bar', '-baz' ],
            wantDownloader: [ 'downloader', '-foo', '-baz' ],
            transcoder: [ 'transcoder', '-foo', '-bar', '-baz' ],
            wantTranscoder: [ 'transcoder', '-foo' ],
            output: [ '-output', '-foo', '-bar', '-baz' ],
            wantOutput: [ '-output', '-foo', '-baz' ]
        ])
    }
}
