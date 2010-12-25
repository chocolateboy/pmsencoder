@Typed
package com.chocolatey.pmsencoder

class MatcherTest extends PMSEncoderTestCase {
    // no match - change nothing
    private void noMatch() {
        assertMatch([
            uri: 'http://www.example.com',
            matches: []
        ])
    }

    // confirm that there are no side-effects that prevent this returning the same result for the same input
    void testIdempotent() {
        noMatch()
        noMatch()
    }

    void testInterpolationInDefaultTrancoderArgs() {
        assertMatch([
            uri:      'http://www.example.com',
            wantArgs: [
                '-prefer-ipv4',
                '-oac', 'lavc',
                '-of', 'lavf',
                '-lavfopts', 'format=dvd',
                '-ovc', 'lavc',
                // make sure nbcores is interpolated here as 3 in threads=3
                // (this is mocked to 3 in PMSEncoderTestCase)
                '-lavcopts', "vcodec=mpeg2video:vbitrate=4096:threads=3:acodec=ac3:abitrate=128",
                '-ofps', '25',
                '-cache', '16384',
                '-vf', 'harddup'
            ],
            useDefaultArgs: true
        ])
    }

    void testApple() {
        assertMatch([
            uri: 'http://www.apple.com/foobar.mov',
            args: [ '-lavcopts', 'vbitrate=4096' ],
            wantArgs: [
                '-lavcopts',   'vbitrate=4096',
                '-ofps',       '24',
                '-user-agent', 'QuickTime/7.6.2'
            ],
            matches: [ 'Apple Trailers' ]
        ])
    }

    /*
        lavcopts look like this:

            -lavcopts vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128

        but for the purposes of this test, this will suffice:

            -lavcopts vbitrate=4096
    */
    void testAppleHD() {
        assertMatch([
            uri: 'http://www.apple.com/foobar.m4v',
            args: [ '-lavcopts', 'vbitrate=4096' ],
            wantArgs: [
                '-lavcopts', 'vbitrate=5086',
                '-ofps', '24',
                '-user-agent', 'QuickTime/7.6.2'
            ],
            matches: [
                'Apple Trailers',
                'Apple Trailers HD'
            ]
        ])
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
        def script = this.getClass().getResource('/youtube_accept.groovy')
        youTubeCommon('34', script)
    }

    private void youTubeCommon(String fmt, URL script = null) {
        def youtube = 'http://www.youtube.com'
        def uri = "$youtube/watch?v=_OBlgSz8sSM"
        assertMatch([
            uri: uri,
            script: script,
            matches: ['YouTube Metadata', 'YouTube-DL Compatible', 'YouTube' ],
            wantStash: { Stash stash ->
                assert stash.keySet().toList() == [
                    '$URI',
                    '$youtube_video_id',
                    '$youtube_t',
                    '$youtube_uploader',
                    '$youtube_dl_compatible',
                    '$youtube_uri',
                    '$youtube_fmt'
                ]

                def video_id = stash['$youtube_video_id']
                assert video_id == '_OBlgSz8sSM'

                def t = stash['$youtube_t']
                // the mysterious $t token changes frequently, but always seems to end in a URL-encoded "="
                assert t =~ /.*%3D$/
                assert stash['$youtube_uploader'] == 'HDCYT'
                assert stash['$youtube_fmt'] == fmt
                assert stash['$youtube_uri'] == uri
                assert stash['$URI'] =~ '\\.youtube\\.com/videoplayback\\?'
                return true
            },
            wantArgs: []
        ])
    }

    void testTED() {
        assertMatch([
            uri: 'http://feedproxy.google.com/~r/TEDTalks_video/~3/EOXWNNyoC3E/843',
            wantArgs: [ '-ofps', '24' ],
            matches: [ 'TED' ]
        ])
    }

    void testGameTrailers() {
        def page_id = '48298'
        def movie_id = '5162'
        def filename = 't_ufc09u_educate_int_gt'
        def uri = "http://www.gametrailers.com/download/$page_id/${filename}.flv"
        def wantURI = "http://trailers-ak.gametrailers.com/gt_vault/${movie_id}/${filename}.flv"

        assertMatch([
            uri: uri,
            wantStash: [
                $URI:                   wantURI,
                $gametrailers_movie_id: movie_id,
                $gametrailers_page_id:  page_id,
                $gametrailers_filename: filename
            ],
            matches: [
                'GameTrailers (Revert PMS Workaround)',
                'GameTrailers',
            ]
        ])
    }
}
