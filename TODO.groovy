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

/*
    use a subclass of GroovyShell that doesn't auto-import java.net.URI or net.pms.PMS so that they
    can be used as a stash var. then make all core vars upper case to distinguish them from user vars:

        ARGS
        EXECUTABLE
        OUTPUT
	PMS
        URI

    Alternatively (and much more lazily) use a dollar prefix (need to check that this works with RegexPlus):
*/

	match $URI: '(?<$URI>http://www.example.com/foo/bar/1234/)unused.xyz'

	$URI = 'http://...'

/* expose a PMS instance - and use it to set nbcores */

    def nbcores = PMS.getConfiguration().getNumberOfCpuCores()

    DEFAULT_MENCODER_ARGS = [ ..., '...:threads=$nbcores:...', ... ]
