@Typed
package com.chocolatey.pmsencoder

class CustomScriptTest extends PMSEncoderTestCase {
    void testOverrideDefaultArgs() {
        def uri = 
        assertMatch([
            script:         '/default_mencoder_args.groovy',
            uri:            'http://www.example.com',
            wantArgs:       [ '-foo', '-bar', '-baz', '-quux' ],
            useDefaultArgs: true
        ])
    }

    // confirm that a profile (YouTube) can be overridden
    void testProfileReplace() {
        def uri = 'http://www.gametrailers.com/video/action-trailer-littlebigplanet-2/708893'
        assertMatch([
            script:        '/profile_replace.groovy',
            uri:           uri,
            wantArgs:       [ '-game', 'trailers' ],
            matches:        [ 'GameTrailers' ],
            useDefaultArgs: false
        ])
    }

    void testProfileAppend() {
        assertMatch([
            script:   '/profile_append.groovy',
            uri:      'http://www.example.com',
            wantArgs: [ '-an', 'example' ],
            matches:  [ 'Example' ]
        ])
    }

    void testGStrings() {
        assertMatch([
            script:    '/gstrings.groovy',
            uri:       'http://www.example.com',
            wantStash: [
                $action:  'Hello, world!',
                $domain:  'example',
                $key:     'key',
                $n:       '41',
                $pattern: 'Hello, world!',
                $URI:     'http://www.example.com/example/key/value/42',
                $value:   'value'
            ],
            wantArgs:  [ '-key', 'key', '-value', 'value' ],
            matches :  [ 'GStrings' ]
        ])
    }

    void testGString() {
        assertMatch([
            script:   '/gstring_scope.groovy',
            uri:      'http://www.example.com',
            wantArgs: [ 'config3', 'profile3', 'pattern3', 'action3' ],
            matches:  [ 'GString Scope' ]
        ])
    }
}
