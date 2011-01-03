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

videofeed {
    uri  = 'http://example.com/rss'
    icon = 'http://example.com/rss.jpg'
    path = '/Web/Foo/Bar/Baz'
}

videostream {
    uri  = 'mms://example.com/stream'
    icon = 'http://example.com/rss.jpg'
    path = 'Quux' // relative path: relative to the previous path i.e. /Web/Foo/Bar/Baz/Quux
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

    Matcher creates a Script in its constructor:

        this.script = new Script(this)

    Matcher receives a command object and passes it to Script.match()

        return script.match(command)

    Script creates an object that holds methods and members common to Patterns and Actions (command delegate)

        def commandDelegate = new CommandDelegate(this, command)

    For each Profile, Script instantiates a Pattern:

        def pattern = new Pattern(commandDelegate)

    If the pattern matches, Script creates an Action:

        def action = new Action(commandDelegate)

    The command response is modified via the command delegate object
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
