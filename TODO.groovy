/* add $mencoder_feature_level (bool) to stash based on MEncoder version */

/* XPath scraping? */

    scrape(
        uri:    uri, // default
        xpath:  '//foo/bar/@baz',
        regex:  'foo:(?<bar>bar):baz'
        format: 'html' // default if xpath is defined
    )

/* make scripts hot-swappable */

/*
    wrap profile/config evaluation in try/catch blocks so that errors can be recovered from
*/

/*
    document the outstanding issue with e.g. "${URI}&has_verified" barfing:

        java.net.URISyntaxException: Illegal character in path at index 5: class java.net.URI&has_verified=1

    investigate using a custom GroovyShell without URI autoimported

*/

// test profile extends: ...

/*
    infinite loop/stack overflow in maven assembly plugin (in Plexus Archiver) with
    Groovy++ 0.2.26: https://groups.google.com/group/groovyplusplus/msg/a765fe77975650db
    revert to 0.2.25 for now
*/

/*
    script management:

        simple: pmsencoder script directory rather than a single script
        better: pmsencoder script repositories i.e. add/remove URIs of PMSEncoder script directories
        disable/enable scripts through the Swing UI (cf. GreaseMonkey)
*/

/*
    fix the sigil mess - the whole thing is a workaround for the URI property conflicting with the class
    groovysh has the same problem, but groovy script.groovy doesn't
    also: https://code.google.com/p/awsgroovyclouds/source/browse/trunk/AWSDrivers/src/com/groovyclouds/aws/S3Driver.groovy#897

        private static final def URI = "URI"
*/

// add an option to allow the logfile location to be specified

// pmsencoder_conf=/path/to/pmsencoder.conf
