@Typed
package com.chocolatey.pmsencoder

class HookTest extends PMSEncoderTestCase {
    void testHookList() {
        def uri = 'http://hook.list'
        assertMatch([
            script: '/hook.groovy',
            uri: uri,
            matches: [ 'Hook List' ],
            hook: [ 'list', uri ]
        ])
    }

    void testHookString() {
        def uri = 'http://hook.string'
        assertMatch([
            script: '/hook.groovy',
            uri: uri,
            matches: [ 'Hook String' ],
            hook: [ 'string', uri ]
        ])
    }
}
