/* add mencoder.feature.level (bool) to stash based on MEncoder version */

/* More matchers: */

    greaterThan java.version: 1.5.0
    gt pms.revision: 400

/* Different matcher syntax: */

    uri ~~ 'http://(?:\\w+\\.)?youtube\\.com/watch\\?v=(?<video_id>[^&]+)'
    pms.revision > 400 

/* XPath scraping: */

    get '//foo/bar/@baz', 'foo:(?<bar>bar):baz'
    get '//foo/bar/text()', 'foo:(?<bar>bar):baz'

// Or:

    get uri:   stash[uri], // default
        xpath: '//foo/bar/@baz',
        regex: 'foo:(?<bar>bar):baz'
        format: 'html' // default if xpath is defined
