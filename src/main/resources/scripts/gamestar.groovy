// videofeed.Web,GameStar=http://www.gamestar.de/videos/rss/videos.rss

script (INIT) { // INIT: pre-empt youtube-dl's "generic" downloader (which doesn't work for this site)
    profile ('GameStar') {
        pattern {
            match uri: '^http://(\\w+\\.)?gamestar\\.de/videos/'
        }

        action {
            // 1) extract the video_src URI
            // e.g. http://www.gamestar.de/jw5/player.swf?config=http://www.gamestar.de/emb/getVideoData5.cfm?vid=71321
            def videoSrc = $('link[rel=video_src]').attr('href')

            // 2) extract the URI of the XML file containing the video's metadata
            // e.g. http://www.gamestar.de/emb/getVideoData5.cfm?vid=71321
            def xmlUri = http.getNameValueMap(videoSrc)['config']

            // 3) extract the video URI from the XML's <file>...</file> element
            // e.g. http://download.gamestar.de/public/70100/70119/Dungeon_of_the_Endless_-_What_s_Behind_the_Door__Teaser_Trai.mp4
            // we can't use "uri = http.getXML(xmlUri).file" because the XML is malformed (the first line is blank),
            // so we fix it up and parse the cleaned-up string
            def xml = http.get(xmlUri).trim() // fix up the malformed XML
            def xmlSlurper = new XmlSlurper()
            uri = xmlSlurper.parseText(xml).file
        }
    }
}
