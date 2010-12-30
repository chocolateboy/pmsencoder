@Typed
package com.chocolatey.pmsencoder

class RejectTest extends PMSEncoderTestCase {
    void setUp() {
        super.setUp()
        def script = this.getClass().getResource('/reject.groovy')
        matcher.load(script)
    }

    void testRejects() {
        assertMatch([
            uri: 'http://www.example.com',
            matches: [
                'Match',
                'Reject Profile',
                'Reject Profiles',
                'Reject Key Unquoted',
                'Reject Key Quoted',
                'Reject Block',
                'Reject (String, String)',
                'Reject (String, List)'
            ]
        ])
    }
}
