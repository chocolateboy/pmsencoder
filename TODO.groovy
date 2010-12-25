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
    wrap profile evaluation in a try/catch block so errors don't affect the rest of the script
*/

/*
    document the outstanding issue with e.g. "${URI}&has_verified" barfing:

        java.net.URISyntaxException: Illegal character in path at index 5: class java.net.URI&has_verified=1

    investigate using a custom GroovyShell without URI autoimported

*/

// 100% test coverage

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
    wire dependencies lazily so that we don't have to re-order/predeclare profiles

    ditto replacements?

    What if something now declares itself as coming after/before (e.g.) YouTube DL?

    FIXME: need some sort of alias map for replacements:

    if we have TED and YouTube:

    profiles:

        key:     profile
        TED:     TED
        YouTube: YouTube

    - and then replace YouTube with YouTube DL:

    profiles:

        key:     profile
        TED:     TED
        YouTube: YouTube DL

    aliases:

        profile:    key
        YouTube DL: YouTube

    XXX We need a topological (rather than lexical) ordering for replacements

    resolve all this in the validate/resolve method (currently Config.verifyDependencies) called from Config.match
*/
