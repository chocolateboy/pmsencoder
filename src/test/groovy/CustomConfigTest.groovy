@Typed
package com.chocolatey.pmsencoder

class CustomConfigTest extends PMSEncoderTestCase {
    void testOverrideDefaultArgs() {
        def customConfig = this.getClass().getResource('/default_mencoder_args.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ URI: uri ])
        def wantCommand = new Command([ URI: uri ], [ '-foo', '-bar', '-baz', '-quux' ])

        matcher.load(customConfig)

        assertMatch(
            command,      // supplied command
            wantCommand,  // expected command
            [],           // expected matches
            true          // use default MEncoder args
        )
    }

    void testProfileReplace() {
        def customConfig = this.getClass().getResource('/profile_replace.groovy')
        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video'
        def command = new Command([ URI: uri ])
        def wantCommand = new Command([ URI: uri + '/foo/bar.baz' ], [ '-foo', 'bar' ])

        matcher.load(customConfig)

        assertMatch(
            command,      // supplied command
            wantCommand,  // expected command
            [ 'TED' ]     // expected matches
        )
    }

    void testProfileAppend() {
        def customConfig = this.getClass().getResource('/profile_append.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ URI: uri ])
        def wantCommand = new Command([ URI: uri ], [ '-an', 'example' ])

        matcher.load(customConfig)

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Example' ], // expected matches
        )
    }
}
