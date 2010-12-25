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

    // confirm that the default TED profile works
    void testProfile() {
        def TEDArgs = new ArrayList<String>(matcher.script.$DEFAULT_TRANSCODER_ARGS) // XXX clone doesn't work
        def index = TEDArgs.findIndexOf { it == '25' } // fps
        assert index > -1
        TEDArgs[index] = '24'

        assertMatch([
            uri:            'http://feedproxy.google.com/~r/TEDTalks_video',
            wantArgs:       TEDArgs,
            matches:        [ 'TED' ],
            useDefaultArgs: true
        ])
    }

    // now confirm that it can be overridden
    void testProfileReplace() {
        def TEDArgs = new ArrayList<String>(matcher.script.$DEFAULT_TRANSCODER_ARGS)
        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video'
        List<String> wantArgs = TEDArgs + [ '-foo', 'bar' ] // type-inference fail

        assertMatch([
            script:        '/profile_replace.groovy',
            uri:           uri,
            wantStash:      [ $URI: uri + '/foo/bar.baz' ],
            wantArgs:       wantArgs,
            matches:        [ 'TED' ],
            useDefaultArgs: true
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

    void testDefaultProfileOverride() {
        def wantArgs = new ArrayList<String>(matcher.script.$DEFAULT_TRANSCODER_ARGS)
        def index = wantArgs.findIndexOf { it == '-lavcopts' }
        assert index > -1
        // make sure nbcores is interpolated here as 3 in threads=3
        wantArgs[ index + 1 ] = 'vcodec=mpeg2video:vbitrate=4096:threads=3:acodec=ac3:abitrate=384'

        assertMatch([
            script:         '/profile_default.groovy',
            uri:            'http://www.example.com',
            wantArgs:       wantArgs,
            matches:        [ 'Default' ],
            useDefaultArgs: true,
        ])
    }
}
