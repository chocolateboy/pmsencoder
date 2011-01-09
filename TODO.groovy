// move all of this to GitHub issues

/* add $mencoder_feature_level (bool) to stash based on MEncoder version */

/* XPath scraping? */

    scrape(
        uri:    uri, // default
        xpath:  '//foo/bar/@baz',
        regex:  'foo:(?<bar>bar):baz'
        format: 'html' // default if xpath is defined
    )

/*
    document the outstanding issue with e.g. "${URI}&has_verified" barfing:

        java.net.URISyntaxException: Illegal character in path at index 5: class java.net.URI&has_verified=1

    investigate using a custom GroovyShell without URI autoimported

*/

/*
    infinite loop/stack overflow in maven assembly plugin (in Plexus Archiver) with
    Groovy++ 0.2.26: https://groups.google.com/group/groovyplusplus/msg/a765fe77975650db
*/

// script management: disable/enable scripts through the Swing UI (cf. GreaseMonkey)

/*
    fix the sigil mess - the whole thing is a workaround for the URI property conflicting with the class
    groovysh has the same problem, but groovy script.groovy doesn't
    also: https://code.google.com/p/awsgroovyclouds/source/browse/trunk/AWSDrivers/src/com/groovyclouds/aws/S3Driver.groovy#897

        private static final def URI = "URI"
*/

/*
  investigate adding seek support for YouTube videos:

      http://stackoverflow.com/questions/3302384/youtubes-hd-video-streaming-server-technology
*/

// WEB.groovy:

videostream ('/Web/TV') {
    uri  = 'mms://example.com/stream'
    icon = 'http://example.com/rss.jpg'
}

// use the user-specified folder, rather than appending the feed name, so feeds can be merged:
root = 'Web'

videofeed ('YouTube/Favourites') {
    uri ([ 'http://youtube.com/api/whatever?1-50', 'http://youtube.com/api/whatever?50-100' ])
    icon = 'http://example.com/rss.jpg'
}

/*

fix the design: http://groovy.codehaus.org/Replace+Inheritance+with+Delegation

Should be:

    Matcher receives an exchange (request/response) object. It creates a
    wrapper that holds methods and members common to Patterns and Actions:

        def exchange = new Exchange(this, request)

    For each Profile, Matcher instantiates a Pattern:

        def pattern = new Pattern(exchange)

    If the pattern matches, it then creates an Action:

        def action = new Action(exchange)

Read-only options could go in request and r/w objects in response e.g.

new Request(
    DOWNLOADER_OUT: ...,
    TRANSCODER_OUT: ...,
    URI: ...,
)

new Response(
    ARGS: ...,
    DOWNLOADER: ...
    TRANSCODER: ...
    URI: ...
)

matcher.run(request, response)

http://stackoverflow.com/questions/325346/name-for-http-requestresponse
http://stackoverflow.com/questions/1039513/what-is-a-request-response-pair-called

*/

// Fix the Script delegate so that globals can be shared e.g.

// unix_paths_example.groovy

script {
    PERL             = '/usr/bin/perl'
    PYTHON           = '/usr/bin/python'
    YOUTUBE_DL       = '/usr/bin/youtube-dl'
    GET_FLASH_VIDEOS = '/usr/bin/get-flash-videos'
}

// make $URI a URI rather than a String?

// tests for prepend and append

// migrate (some) regex scrapers to Geb (or Geb + regex)

// when documenting scripting, note poor man's script versioning via Github's "Switch Tags" menu

// a suite of scrapers and extractors:

    scrape:  regex
    browse:  Geb
    query:   Doj and/or port Zombie.js
    xpath:   HtmlUnit?

// use block syntax for scrape?

    scrape { 'foo/bar(?<baz>\\w+)' }
    scrape (uri: uri) { 'foo/bar(?<baz>\\w+)' }

// download/install standard library (GitHub API/JGit):

    pmsencoder.library.root = /tmp/pmsencoder/scripts
    pmsencoder.library.repository = http://example.com/path/to/scripts,http://...

// asynchronously downloads + installs scripts for this version of PMSEncoder and saves them to the specified
// (writable) location under the version

    /tmp/pmsencoder/scripts/1.3.0
    /tmp/pmsencoder/scripts/1.4.0

// Do this by default and eliminate the built-in pmsencoder.groovy?

// query youtube-dl and get-flash-videos for supported sites at startup

// add (overridable) INIT.groovy (or DEFAULT.groovy) for one-off initializations (e.g. $DEFAULT_MENCODER_ARGS)

// print a debug version of the MEncoder (if used) command-line (i.e. pump up the debug level), target e.g.
// deleteme.tmp, and quote the URI (i.e. would need to be done in PMSEncoder.groovy)

// add namespace support (required?):

    script (namespace: 'http://www.example.com', author: 'chocolateboy') {

// use a web interface because a) Swing sucks and b) headless servers. Only use swing to enable/disable the web server
// and set the port.
