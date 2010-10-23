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
    Groovy++ 0.2.26

    assembling with:

        mvn -X assembly:single

    works around it for now (presumably by distracting it from its concurrency blues with debug noise)

*/
