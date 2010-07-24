config {
    // the default MEncoder args - these can be redefined in a custom config file
    args = [
        '-prefer-ipv4',
        '-oac', 'lavc',
        '-of', 'lavf',
        '-lavfopts', 'format=dvd',
        '-ovc', 'lavc',
        '-lavcopts', 'vcodec=mpeg2video:vbitrate=4096:threads=2:acodec=ac3:abitrate=128',
        '-ofps', '25',
        '-cache', '16384', // default cache size; default minimum percentage is 20%
        '-vf', 'harddup'
    ]

    /*
        this is the default list of YouTube format/resolution IDs we should accept/select - in descending
        order of preference.

        it can be modified here to add/remove a format, or can be overridden on a per-video basis
        by supplying a new list to the youtube method (see below) e.g.
        
        exclude '1080p':

            youtube ytaccept - [ 37 ]

        add '2304p':

            youtube [ 38 ] + ytaccept

	For the full list of formats, see: https://secure.wikimedia.org/wikipedia/en/wiki/YouTube#Quality_and_codecs
    */

    ytaccept = [
        '37',  // 1080p
        '22',  // 720p
        '35',  // 480p
        '34',  // 360p
	'18',  // Medium
        '5'    // 240p
    ]

    profile ('YouTube') {
        // extract the resource's video_id from the URI of the standard YouTube page
        pattern {
            match uri: '^http://(?:\\w+\\.)?youtube\\.com/watch\\?v=(?<video_id>[^&]+)'
        }

        action {

              /*
                  Now, with $video_id defined, call the custom YouTube handler.

                  For now, call it with no arguments and leave it to select the
                  highest available resolution. But in future this can be
                  refined to allow the user to limit/default the resolution
                  (e.g. to save bandwidth/reduce download time) in a custom config file.
                  See above.
               */

	      youtube()
        }
    }
                  
    profile ('Apple Trailers') {
        pattern {
            match uri: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        // FIXME: 4096 is a needlessly high video bitrate; they typically weigh in at ~1200 Kbps
        action {
            set ofps: '24', 'user-agent': 'QuickTime/7.6.2'
        }
    }

    profile ('Apple Trailers HD') {
        pattern {
            match uri: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+\\.m4v$'
        }
        
        action {
            replace lavcopts: [ '4096': '5086' ] // increase the bitrate
        }
    }

    profile ('TED') {
        pattern {
            match uri: '^http://feedproxy\\.google\\.com/~r/TEDTalks_video\\b'
        }
        
        action {
            set ofps: '24'
        }
    }

    profile ('GameTrailers (Revert PMS Workaround)') {
        /*
           convert:

               http://www.gametrailers.com/download/48298/t_ufc09u_educate_int_gt.flv

           to:

               http://www.gametrailers.com/player/48298.html
         */

        // 1) extract the page ID
        pattern {
            match uri: '^http://(www\\.)?gametrailers\\.com/download/(?<page_id>\\d+)/[^.]+\\.flv$'
        }
        
        // 2) and use it to restore the correct webpage URI
        action {
            let uri: 'http://www.gametrailers.com/player/$page_id.html'
        }
    }

    profile ('GameTrailers') {
        pattern {
            match uri: '^http://(www\\.)?gametrailers\\.com/'
        }
        
        action {
            /*
                The order is important here! Make sure we scrape the variables before we set the URI.
                extract some values from the HTML
            */
            scrape '\\bmov_game_id\\s*=\\s*(?<movie_id>\\d+)'
            scrape '\\bhttp://www\\.gametrailers\\.com/download/\\d+/(?<filename>t_[^.]+)\\.wmv\\b'

            // now use them to rewrite the URI
            let uri: 'http://trailers-ak.gametrailers.com/gt_vault/$movie_id/$filename.flv'
        }
    }
}
