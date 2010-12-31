@Typed
package com.chocolatey.pmsencoder

class ProfileTest extends PMSEncoderTestCase {
    void testOverrideDefaultArgs() {
        assertMatch([
            script:         '/default_mencoder_args.groovy',
            uri:            'http://www.example.com',
            wantArgs:       [ '-default', '-mencoder', '-args' ],
            useDefaultArgs: true
        ])
    }

    // confirm that a profile (GameTrailers) can be overridden
    void testProfileOverride() {
        def uri = 'http://www.gametrailers.com/video/action-trailer-littlebigplanet-2/708893'
        assertMatch([
            script:        '/profile_override.groovy',
            uri:           uri,
            wantArgs:       [ '-game', 'trailers' ],
            matches:        [ 'GameTrailers' ],
            useDefaultArgs: false
        ])
    }

    // ditto, but using the 'replaces' keyword and a different profile name
    void testProfileReplace() {
        def uri = 'http://www.gametrailers.com/video/action-trailer-littlebigplanet-2/708893'
        assertMatch([
            script:        '/profile_replace.groovy',
            uri:           uri,
            wantArgs:       [ '-gametrailers', 'replacement' ],
            matches:        [ 'GameTrailers Replacement' ],
            useDefaultArgs: false
        ])
    }

    void testInheritPattern() {
        assertMatch([
            script:   '/profile_extend.groovy',
            uri:      'http://inherit.pattern',
            wantArgs: [ '-base', '-inherit', 'pattern' ],
            matches:  [ 'Base', 'Inherit Pattern' ]
        ])
    }

    void testInheritAction() {
        assertMatch([
            script:   '/profile_extend.groovy',
            uri:      'http://inherit.action',
            wantArgs: [ '-base' ],
            matches:  [ 'Inherit Action' ]
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
