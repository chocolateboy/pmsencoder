@Typed
package com.chocolatey.pmsencoder

class ArgsTest extends PMSEncoderTestCase {
    void testArgsList() {
        def uri = 'http://args.list'
        assertMatch([
            script: '/args.groovy',
            uri: uri,
            matches: [ 'Args List' ],
            downloader: [ 'list', uri ]
        ])
    }

    void testArgsString() {
        def uri = 'http://args.string'
        assertMatch([
            script: '/args.groovy',
            uri: uri,
            matches: [ 'Args String' ],
            downloader: [ 'string', uri ]
        ])
    }
}
