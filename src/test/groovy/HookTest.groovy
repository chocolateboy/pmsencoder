@Typed
package com.chocolatey.pmsencoder

class HookTest extends PMSEncoderTestCase {
    void testHookString() {
        def uri = 'http://hook.string'
        assertMatch([
            script: '/hook.groovy',
            uri: uri,
            wantMatches: [ 'Hook String' ],
            wantHook: [ 'string', uri ]
        ])
    }

    void testHookList() {
        def uri = 'http://hook.list'
        assertMatch([
            script: '/hook.groovy',
            uri: uri,
            wantMatches: [ 'Hook List' ],
            wantHook: [ 'list', uri ]
        ])
    }
}
