@Typed
package com.chocolatey.pmsencoder

class PatternTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def customConfig = this.getClass().getResource('/pattern.groovy')
        matcher.load(customConfig)
    }

    void testPatternMatchBlock() {
        def uri = 'http://foo.bar.baz'
        def command = new Command([ '$URI': uri ])
        def wantCommand = new Command(
            [ '$URI': uri, eq: uri ], []
        )


        assertMatch(
            command,       // supplied command
            wantCommand,   // expected command
            [ 'Eq' ],      // expected matches
        )
    }

    void testMatches() {
        def uri = 'http://trailers.apple.com/movies/fox_searchlight/127hours/127hours-tlr1_h720p.mov'
        def command = new Command([ '$URI': uri ])
        def wantCommand = new Command(
            [ '$URI': uri, id: 'Apple 3' ], [ '-ofps', '24', '-user-agent', 'QuickTime/7.6.2' ]
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
