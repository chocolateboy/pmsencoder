/* add mencoder.feature.level (bool) to stash based on MEncoder version */

/* More patterns? */

    greaterThan java.version: 1.5.0
    gt pms.revision: 400

/* Different pattern syntax? */

    uri ~~ 'http://(?:\\w+\\.)?youtube\\.com/watch\\?v=(?<video_id>[^&]+)'
    pms.revision > 400 

/* XPath scraping? */

    scrape '//foo/bar/@baz', 'foo:(?<bar>bar):baz'
    scrape '//foo/bar/text()', 'foo:(?<bar>bar):baz'

/* Or: */

    scrape(
        uri:    uri, // default
        xpath:  '//foo/bar/@baz',
        regex:  'foo:(?<bar>bar):baz'
        format: 'html' // default if xpath is defined
    )

/* restore missing actions e.g. add and remove */

/* Add tests for ytaccept */

/* add the list of matched profiles to the command object and add e.g. a matched method to query it */

    pattern ('Custom Youtube') {
        matched any: 'YouTube'
        matched all: [ 'YouTube', 'YouTube HD' ]
    }
