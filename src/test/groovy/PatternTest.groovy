@Typed
package com.chocolatey.pmsencoder

class PatternTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/pattern.groovy')
        matcher.load(script)
    }

    void testPatternMatchBlock() {
        def uri = 'http://foo.bar.baz'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri, eq: uri ], []
        )


        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Eq' ],      // expected matches
        )
    }

    void testPatternMatchKeyString() {
        def uri = 'http://key.string.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri ], []
        )

        assertMatch(
            command,          // supplied command
            wantCommand,      // expected command
            [ 'Key String' ], // expected matches
        )
    }

    void testPatternMatchKeyList() {
        def uri = 'http://key.list.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri ], []
        )

        assertMatch(
            command,        // supplied command
            wantCommand,    // expected command
            [ 'Key List' ], // expected matches
        )
    }

    void testPatternMatchStringString() {
        def uri = 'http://string.string.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri ], []
        )

        assertMatch(
            command,             // supplied command
            wantCommand,         // expected command
            [ 'String String' ], // expected matches
        )
    }

    void testPatternMatchStringList() {
        def uri = 'http://string.list.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri ], []
        )

        assertMatch(
            command,           // supplied command
            wantCommand,       // expected command
            [ 'String List' ], // expected matches
        )
    }

    void testMatches() {
        def uri = 'http://trailers.apple.com/movies/fox_searchlight/127hours/127hours-tlr1_h720p.mov'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [ $URI: uri, profile: 'Apple 3' ], [ '-ofps', '24', '-user-agent', 'QuickTime/7.6.2' ]
        )

        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [              // expected matches
                'Apple Trailers',
                'Apple Trailers HD',
                'Apple 3'
            ],
        )
    }
}
