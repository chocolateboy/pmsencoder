package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class ProfileTest extends PMSEncoderTestCase {
    void testOverrideDefaultArgs() {
        assertMatch([
            script:         '/default_ffmpeg_args.groovy',
            uri:            'http://www.example.com',
            wantTranscoder: [ '-default', '-ffmpeg', '-args' ],
            useDefaultTranscoder: true
        ])
    }

    // confirm that a profile (GameTrailers) can be overridden
    void testProfileOverride() {
        def uri = 'http://www.gametrailers.com/video/action-trailer-littlebigplanet-2/708893'
        assertMatch([
            script:         '/profile_override.groovy',
            uri:            uri,
            wantTranscoder: [ '-game', 'trailers' ],
            wantMatches:    [ 'GameTrailers' ]
        ])
    }

    // ditto, but using the 'replaces' keyword and a different profile name
    void testProfileReplace() {
        def uri = 'http://www.gametrailers.com/video/action-trailer-littlebigplanet-2/708893'
        assertMatch([
            script:         '/profile_replace.groovy',
            uri:            uri,
            wantTranscoder: [ '-gametrailers', 'replacement' ],
            wantMatches:    [ 'GameTrailers Replacement' ]
        ])
    }

    void testInheritPattern() {
        assertMatch([
            script:         '/profile_extend.groovy',
            uri:            'http://inherit.pattern',
            wantTranscoder: [ '-base', '-inherit', 'pattern' ],
            wantMatches:    [ 'Base', 'Inherit Pattern' ]
        ])
    }

    void testInheritAction() {
        assertMatch([
            script:   '/profile_extend.groovy',
            uri:      'http://inherit.action',
            wantTranscoder: [ '-base' ],
            wantMatches:    [ 'Inherit Action' ]
        ])
    }

    void testGStrings() {
        assertMatch([
            script:    '/gstrings.groovy',
            uri:       'http://www.example.com',
            wantStash: [
                action:  'Hello, world!',
                domain:  'example',
                key:     'key',
                n:       41,
                pattern: 'Hello, world!',
                uri:     'http://www.example.com/example/key/value/42',
                value:   'value'
            ],
            wantTranscoder: [ '-key', 'key', '-value', 'value' ],
            wantMatches :   [ 'GStrings' ]
        ])
    }

    void testGString() {
        assertMatch([
            script:   '/gstring_scope.groovy',
            uri:      'http://www.example.com',
            wantTranscoder: [ 'config3', 'profile3', 'pattern3', 'action3' ],
            wantMatches:    [ 'GString Scope' ]
        ])
    }
}
