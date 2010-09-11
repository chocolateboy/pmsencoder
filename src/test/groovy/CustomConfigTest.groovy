@Typed
package com.chocolatey.pmsencoder

class CustomConfigTest extends PMSEncoderTestCase {
    void testOverrideDefaultArgs() {
        def customConfig = this.getClass().getResource('/default_mencoder_args.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ '$URI': uri ])
        def wantCommand = new Command([ '$URI': uri ], [ '-foo', '-bar', '-baz', '-quux' ])

        matcher.load(customConfig)

        assertMatch(
            command,      // supplied command
            wantCommand,  // expected command
            [],           // expected matches
            true          // use default MEncoder args
        )
    }

    // confirm that the default TED profile works
    void testProfile() {
        /// XXX clone doesn't work
        def TEDArgs = new ArrayList<String>(matcher.config.$DEFAULT_MENCODER_ARGS)
        def index = TEDArgs.findIndexOf { it == '25' }

        assert index > -1 // power assert!
        TEDArgs[index] = '24'

        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video'
        def command = new Command([ '$URI': uri ])
        def wantCommand = new Command([ '$URI': uri ], TEDArgs)

        assertMatch(
            command,      // supplied command
            wantCommand,  // expected command
            [ 'TED' ],    // expected matches
            true          // use default args
        )
    }

    // now confirm that it can be overridden
    void testProfileReplace() {
        def customConfig = this.getClass().getResource('/profile_replace.groovy')
        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video'
        def command = new Command([ '$URI': uri ])
        def TEDArgs = new ArrayList<String>(matcher.config.$DEFAULT_MENCODER_ARGS)
        def wantArgs = (TEDArgs + [ '-foo', 'bar' ]) as List<String> // FIXME: type-inference fail (or use Scala)
        def wantCommand = new Command([ '$URI': uri + '/foo/bar.baz' ], wantArgs)

        matcher.load(customConfig)

        assertMatch(
            command,      // supplied command
            wantCommand,  // expected command
            [ 'TED' ],    // expected matches
            true          // use default args
        )
    }

    void testProfileAppend() {
        def customConfig = this.getClass().getResource('/profile_append.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ '$URI': uri ])
        def wantCommand = new Command([ '$URI': uri ], [ '-an', 'example' ])

        matcher.load(customConfig)

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Example' ], // expected matches
        )
    }

    void testGStrings() {
        def customConfig = this.getClass().getResource('/gstrings.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ '$URI': uri ])
        def wantCommand = new Command(
            [
                action:  'Hello, world!',
                domain:  'example',
                key:     'key',
                n:       '41',
                pattern: 'Hello, world!',
                $URI:    'http://www.example.com/example/key/value/42',
                value:   'value'
            ],
            [ '-key', 'key', '-value', 'value' ]
        )

        matcher.load(customConfig)

        assertMatch(
            command,        // supplied command
            wantCommand,    // expected command
            [ 'GStrings' ], // expected matches
        )
    }

    void testGString() {
        def customConfig = this.getClass().getResource('/gstring_scope.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ '$URI': uri ])
        def wantCommand = new Command([ '$URI': uri ], [ 'config3', 'profile3', 'pattern3', 'action3' ])

        matcher.load(customConfig)

        assertMatch(
            command,             // supplied command
            wantCommand,         // expected command
            [ 'GString Scope' ], // expected matches
        )
    }

    void testInterpolationInDefaultMEncoderArgs() {
        def customConfig = this.getClass().getResource('/gstring_scope.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ '$URI': uri ])
        def wantArgs = [
            '-prefer-ipv4',
            '-oac', 'lavc',
            '-of', 'lavf',
            '-lavfopts', 'format=dvd',
            '-ovc', 'lavc',
            // make sure nbcores is interpolated here as 3 in threads=3
            '-lavcopts', "vcodec=mpeg2video:vbitrate=4096:threads=3:acodec=ac3:abitrate=128",
            '-ofps', '25',
            '-cache', '16384',
            '-vf', 'harddup'
        ]

        def wantCommand = new Command([ '$URI': uri ], wantArgs)

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [],            // expected matches
            true           // use default args
        )
    }

    void testDefaultProfileOverride() {
        def customConfig = this.getClass().getResource('/profile_default.groovy')
        def uri = 'http://www.example.com'
        def command = new Command([ '$URI': uri ])
        def wantArgs = [
            '-prefer-ipv4',
            '-oac', 'lavc',
            '-of', 'lavf',
            '-lavfopts', 'format=dvd',
            '-ovc', 'lavc',
            // make sure nbcores is interpolated here as 3 in threads=3
            '-lavcopts', "vcodec=mpeg2video:vbitrate=4096:threads=3:acodec=ac3:abitrate=384",
            '-ofps', '25',
            '-cache', '16384',
            '-vf', 'harddup'
        ]

        def wantCommand = new Command([ '$URI': uri ], wantArgs)
        matcher.load(customConfig)

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Default' ], // expected matches
            true           // use default args
        )
    }
}
