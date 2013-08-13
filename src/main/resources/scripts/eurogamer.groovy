// videofeed.Web,Eurogamer=http://rss.feedsportal.com/feed/eurogamer/eurogamer_tv

script {
    profile ('Eurogamer') {
        pattern { domain 'eurogamer.net' }

        // http://www.eurogamer.net/videos/sc2-heart-of-the-swarm-blizzcon-trailer
        // http://www.eurogamer.net/tv/playlist/110585
        // http://videos.eurogamer.net/47006bb3917b8da6218d491d43493033/500ae168/starcraftiiheartoftheswarmpreviewtrailernewunits-ibuvff6xrkm_stream_h264v2_hd.mp4
        action {
            def playlist_id = $('div[id^=video-block-]').attr('id').match(/(\d+)$/)[0]
            def json = http.getJSON("http://www.eurogamer.net/tv/playlist/${playlist_id}")
            uri = json[0]['hd.file']
        }
    }
}
