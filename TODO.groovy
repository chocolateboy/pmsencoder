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
