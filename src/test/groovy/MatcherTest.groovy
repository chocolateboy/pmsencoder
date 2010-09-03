@Typed
package com.chocolatey.pmsencoder

class MatcherTest extends PMSEncoderTestCase {
    // no match - change nothing
    private void noMatch() {
        def uri = 'http://www.example.com'
        def command = new Command([ uri: uri ])
        def wantCommand = new Command([ uri: uri ])

        assertMatch(
            command,      // supplied command
            wantCommand,  // expected command
            [],           // expected matches
        )
    }

    // confirm that there are no side-effects that prevent this returning the same result for the same input
    void testIdempotent() {
        noMatch()
        noMatch()
    }

    void testApple() {
        def uri = 'http://www.apple.com/foobar.mov'
        def command = new Command([ uri: uri ], [ '-lavcopts', 'vbitrate=4096' ])
        def wantCommand = new Command(
            [ uri: uri ],
            [
                '-lavcopts', 'vbitrate=4096',
                '-ofps', '24',
                '-user-agent', 'QuickTime/7.6.2'
            ]
        )

        assertMatch(
            command,              // supplied command
            wantCommand,          // expected command
            [ 'Apple Trailers' ], // expected matches
        )
    }

    void testAppleHD() {
        def uri = 'http://www.apple.com/foobar.m4v'
        def command = new Command([ uri: uri ], [ '-lavcopts', 'vbitrate=4096' ])
        def wantCommand = new Command(
            [ uri: uri ],
            [
                '-lavcopts', 'vbitrate=5086',
                '-ofps', '24',
                '-user-agent', 'QuickTime/7.6.2'
            ]
        )

        /*
            lavcopts look like this:
            
                -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128

            but for the purposes of this test, this will suffice:
            
                -lavcopts vbitrate=4096
        */

        assertMatch(
            command,
            wantCommand,
            [
                'Apple Trailers',
                'Apple Trailers HD'
            ]
        )
    }

    /*
        we can't use assertMatch here due to the volatility of the token (and possibly)
        the highest available resolution.
    */
    void testYouTube() {
        def youtube = 'http://www.youtube.com'
        def uri = "$youtube/watch?v=_OBlgSz8sSM"
        def command = new Command([ uri: uri ])

        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default MEncoder args

        def stash = command.stash
        def args = command.args
        def matches = command.matches

        assertEquals([ 'YouTube' ], matches)
        assertEquals([
            'uri',
            'youtube_author',
            'youtube_fmt',
            'youtube_t',
            'youtube_uri',
            'youtube_video_id'
        ], stash.keySet().toList().sort())

        def video_id = stash['youtube_video_id']
        assertEquals('_OBlgSz8sSM', video_id)
        def t = stash['youtube_t']
        // the mysterious $t token changes frequently, but always seems to end in a URL-encoded "="
        assert t ==~ /.*%3D$/
        assertEquals('HDCYT', stash['youtube_author'])
        assertEquals('35', stash['youtube_fmt'])
        assertEquals(uri, stash['youtube_uri'])
        def want_uri = "$youtube/get_video?fmt=35&video_id=$video_id&t=$t&asv="
        assertEquals(want_uri, stash['uri'])
        assertEquals([], args)
    }

    void testTED() {
        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video/~3/EOXWNNyoC3E/843'
        def command = new Command([ uri: uri ])
        def wantCommand = new Command([ uri: uri ], [ '-ofps', '24' ])

        assertMatch(
            command,
            wantCommand,
            [ 'TED' ]
        )
    }

    void testGameTrailers() {
        def page_id = '48298'
        def filename = 't_ufc09u_educate_int_gt'
        def uri = "http://www.gametrailers.com/download/$page_id/${filename}.flv"
        def movie_id = '5162'
        def want_uri = "http://trailers-ak.gametrailers.com/gt_vault/$movie_id/${filename}.flv"
        def command = new Command([ uri: uri ])
        def wantCommand = new Command(
            [
                uri:      want_uri,
                movie_id: movie_id,
                page_id:  page_id,
                filename: filename
            ]
        )

        assertMatch(
            command,
            wantCommand,
            [
                'GameTrailers (Revert PMS Workaround)',
                'GameTrailers',
            ]
        )
    }
}
