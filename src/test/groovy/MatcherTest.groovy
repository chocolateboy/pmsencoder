@Typed
package com.chocolatey.pmsencoder

class MatcherTest extends PMSEncoderTestCase {
    // no match - change nothing
    private void noMatch() {
        def uri = 'http://www.example.com'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command([ $URI: uri ])

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
        def command = new Command([ $URI: uri ], [ '-lavcopts', 'vbitrate=4096' ])
        def wantCommand = new Command(
            [ $URI: uri ],
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
        def command = new Command([ $URI: uri ], [ '-lavcopts', 'vbitrate=4096' ])
        def wantCommand = new Command(
            [ $URI: uri ],
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
        youTubeCommon('35')
    }

    // verify that globally modifying $YOUTUBE_ACCEPT works
    void testYOUTUBE_ACCEPT() {
        def customConfig = this.getClass().getResource('/youtube_accept.groovy')
        youTubeCommon('34', customConfig)
    }

    private void youTubeCommon(String fmt, URL customConfig = null) {
        def youtube = 'http://www.youtube.com'
        def uri = "$youtube/watch?v=_OBlgSz8sSM"
        def command = new Command([ $URI: uri ])

        if (customConfig != null) {
            matcher.load(customConfig)
        }

        // bypass Groovy's annoyingly loose definition of true
        assertSame(true, matcher.match(command, false)) // false: don't use default transcoder args

        def stash = command.stash
        def args = command.args
        def matches = command.matches

        assertEquals(['YouTube Metadata', 'YouTube-DL Compatible', 'YouTube' ], matches)
        assertEquals([
            '$URI',
            '$youtube_video_id',
            '$youtube_t',
            '$youtube_uploader',
            '$youtube_dl_compatible',
            '$youtube_uri',
            '$youtube_fmt'
        ], stash.keySet().toList())

        def video_id = stash['$youtube_video_id']
        assertEquals('_OBlgSz8sSM', video_id)
        def t = stash['$youtube_t']
        // the mysterious $t token changes frequently, but always seems to end in a URL-encoded "="
        assert t ==~ /.*%3D$/
        assertEquals('HDCYT', stash['$youtube_uploader'])
        assertEquals(fmt, stash['$youtube_fmt'])
        assertEquals(uri, stash['$youtube_uri'])
        assert stash['$URI'] =~ '\\.youtube\\.com/videoplayback\\?'
        assertEquals([], args)
    }

    void testTED() {
        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video/~3/EOXWNNyoC3E/843'
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command([ $URI: uri ], [ '-ofps', '24' ])

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
        def wantURI = "http://trailers-ak.gametrailers.com/gt_vault/$movie_id/${filename}.flv"
        def command = new Command([ $URI: uri ])
        def wantCommand = new Command(
            [
                $URI:                   wantURI,
                $gametrailers_movie_id: movie_id,
                $gametrailers_page_id:  page_id,
                $gametrailers_filename: filename
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
