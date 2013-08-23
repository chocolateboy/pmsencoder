package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
class MatchTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/match.groovy')
        matcher.load(script)
    }

    void testMatches() {
        assertMatch([
            uri: 'http://www.example.com',
            wantMatches: [
                'Match',
                'Match Profile',
                'Match Profiles',
                'Match Key Unquoted',
                'Match Key Quoted',
                'Match Block',
                'Match (String, String)',
                'Match (String, List)'
            ]
        ])
    }
}
