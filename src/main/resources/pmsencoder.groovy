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

    profile ('YouTube') {
        // extract the resource's video_id from the URI of the standard YouTube page
        match {
            matches uri: '^http://(?:\\w+\\.)?youtube\\.com/watch\\?v=(?<video_id>[^&]+)'
        }

        action {
              // extract the resource's sekrit identifier ($t) from the HTML
              get '&t=(?<t>[^&]+)'

              /*
                  now, with $video_id and $t defined, call the custom YouTube
                  handler with a list of formats in descending order of preference
              */
              youtube '22', '18', '6', '5'
        }
    }
                  
    profile ('Apple Trailers') {
        match {
            matches uri: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+$'
        }

        // FIXME: 4096 is a needlessly high video bitrate; they typically weigh in at ~1200 Kbps
        action {
            set ofps: '24', 'user-agent': 'QuickTime/7.6.2'
        }
    }

    profile ('Apple Trailers HD') {
        match {
            matches uri: '^http://(?:(?:movies|www|trailers)\\.)?apple\\.com/.+\\.m4v$'
        }
        
        action {
            replace lavcopts: [ '4096': '5086' ] // increase the bitrate
        }
    }

    profile ('TED') {
        match {
            matches uri: '^http://feedproxy\\.google\\.com/~r/TEDTalks_video\\b'
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
        match {
            matches uri: '^http://(www\\.)?gametrailers\\.com/download/(?<page_id>\\d+)/[^.]+\\.flv$'
        }
        
        // 2) and use it to restore the correct webpage URI
        action {
            let uri: 'http://www.gametrailers.com/player/$page_id.html'
        }
    }

    profile ('GameTrailers') {
        match {
            matches uri: '^http://(www\\.)?gametrailers\\.com/'
        }
        
        action {
            /*
                The order is important here! Make sure we get the variables before we set the URI.
                extract some values from the HTML
            */
            get '\\bmov_game_id\\s*=\\s*(?<movie_id>\\d+)'
            get '\\bhttp://www\\.gametrailers\\.com/download/\\d+/(?<filename>t_[^.]+)\\.wmv\\b'

            // now use them to rewrite the URI
            let uri: 'http://trailers-ak.gametrailers.com/gt_vault/$movie_id/$filename.flv'
        }
    }
}
