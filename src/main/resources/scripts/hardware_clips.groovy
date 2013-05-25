// videofeed.Web,PCGames=http://www.pcgames.de/feed.cfm?menu_alias=Videos/
// videofeed.Web,Hardware Clips=http://www.hardwareclips.com/rss/new/

script {
    profile ('HardwareClips Whitelabels') {
        pattern {
            domains([ 'hardwareclips.com', 'pcgames.de' ])
        }

        // source: <embed flashvars='config=http://videos.pcgames.de/embed/pcgames/7aaa99f9804d0905fe58/' />
        // source: 'url': 'http://videos.pcgames.de:8080/405a31b06b9ece50772e9c4d9f04826d/4899_mini.mp4',
        // target: http://videos.pcgames.de:8080/405a31b06b9ece50772e9c4d9f04826d/4899_mini.mp4
        action {
            // the playlist URL is defined in an embed's @flashvars attribute, but these embeds may appear
            // inside textareas, in which case jSoup escapes the markup - &lt;embed &c. - so those embeds
            // don't appear in the DOM. so scrape instead
            scrape """\\bflashvars=(["'])config=(?<playlistURI>[^'"]+)\\1"""

            // the playlist is invalid JSON (uses single quotes), so we need to scrape again
            scrape uri: playlistURI, "url.+?'url':\\s*'(?<uri>[^']+)'"
        }
    }
}
