@Typed
package com.chocolatey.pmsencoder

class MatcherTest extends PMSEncoderTestCase {
    // no match - change nothing
    private void noMatch() {
        def uri = 'http://www.example.com'
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(uri: uri)

        assertMatch(
            uri,          // URI
            stash,        // stash
            [],           // args
            [],           // expected matches
            want_stash,   // expected stash
            []            // expected args
        )
    }

    // confirm that there are no side-effects that prevent this returning the same result for the same input
    void testIdempotent() {
        noMatch()
        noMatch()
    }

    void testApple() {
        def uri = 'http://www.apple.com/foobar.mov'
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(uri: uri)

        assertMatch(
            uri,                                 // URI
            stash,                               // stash
            [ '-lavcopts', 'vbitrate=4096' ],    // args
            [ 'Apple Trailers' ],                // expected matches
            want_stash,                          // expected stash
            [                                    // expected args
                '-lavcopts', 'vbitrate=4096',
                '-ofps', '24',
                '-user-agent', 'QuickTime/7.6.2'
            ]
        )
    }

    void testAppleHD() {
        def uri = 'http://www.apple.com/foobar.m4v'
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(uri: uri)

        /*
            lavcopts look like this:
            
                -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128

            but for the purposes of this test, this will suffice:
            
                -lavcopts vbitrate=4096
        */

        assertMatch(
            uri,                                  // URI
            stash,                                // stash
            [ '-lavcopts', 'vbitrate=4096' ],     // args
            [                                     // expected matches
                'Apple Trailers',
                'Apple Trailers HD'
            ],
            want_stash,                           // expected stash
            [                                     // expected args
                '-lavcopts', 'vbitrate=5086',
                '-ofps', '24',
                '-user-agent', 'QuickTime/7.6.2'
            ]
        )
    }

    /*
        we can't use assertMatch here due to the dynamic nature of YouTube's result.
        instead, do a basic sanity-check that the URI contains youtube.com/videoplayback?
    */
    void testYouTube() {
        def youtube = 'http://www.youtube.com'
        def uri = "$youtube/watch?v=_OBlgSz8sSM"
        def stash = new Stash(uri: uri)
        def args = []

        List<String> matches = matcher.match(stash, args, false)

        assert matches == [ 'YouTube' ]
        assert stash.keySet().toList() == [ 'uri', 'video_id', 't' ]
        def video_id = stash['video_id']
        assert video_id == '_OBlgSz8sSM'
        def t = stash['t']
	// the mysterious $t token changes frequently, but always seems to end in a URL-encoded "="
        assert t ==~ /.*%3D$/
        def want_uri = "$youtube/get_video?fmt=35&video_id=$video_id&t=$t&eurl=&el=&ps=&asv="
        assert stash['uri'] == want_uri
        assert args == []
    }

    void testTED() {
        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video/~3/EOXWNNyoC3E/843'
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(uri: uri)

        assertMatch(
            uri,                                  // URI
            stash,                                // stash
            [],                                   // args
            [ 'TED' ],                            // expected matches
            want_stash,                           // expected stash
            [                                     // expected args
                '-ofps', '24'
            ]
        )
    }

    void testGameTrailers() {
        def page_id = '48298'
        def filename = 't_ufc09u_educate_int_gt'
        def uri = "http://www.gametrailers.com/download/$page_id/${filename}.flv"
        def movie_id = '5162'
        def want_uri = "http://trailers-ak.gametrailers.com/gt_vault/$movie_id/${filename}.flv"
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(
            uri:      want_uri,
            movie_id: movie_id,
            page_id:  page_id,
            filename: filename
        )

        assertMatch(
            uri,                                  // URI
            stash,                                // stash
            [],                                   // args
            [                                     // expected matches
                'GameTrailers (Revert PMS Workaround)',
                'GameTrailers',
            ],
            want_stash,                           // expected stash
            []                                    // expected args
        )
    }
}
