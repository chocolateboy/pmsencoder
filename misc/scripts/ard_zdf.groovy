/*
    videofeed.Web,ZDF=http://www.zdf.de/ZDFmediathek/rss/562?view=rss
    videofeed.Web,3sat=http://www.3sat.de/mediathek/rss/mediathek_scobel.xml
    videofeed.Web,3sat=http://www.3sat.de/mediathek/rss/mediathek_boerse.xml
    videofeed.Web,3sat=http://www.3sat.de/mediathek/rss/mediathek_hitec.xml
*/

script {
    profile ('ARD/ZDF') {
        pattern {
            match $URI: '^http://hstreaming\\.(ard|zdf)\\.de/.+?\\.mov$'
        }

        action {
            // scrape the RTSP URI from the .mov "container" (actually a plain text file)
            scrape '(?<URI>rtsp://\\S+)'
            // now set the correct MEncoder options
            set '-rtsp-stream-over-tcp' // needed for some firewalls/routers
        }
    }
}
