/* add mencoder.feature.level (bool) to stash based on MEncoder version */

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

/* Add tests for ytaccept */

/* add the list of matched profiles to the command object so that it can be queried */

    pattern ('Custom Youtube') {
        match { 'YouTube' in matched }
        match { [ 'YouTube', 'YouTube HD' ] in matched }
    }
