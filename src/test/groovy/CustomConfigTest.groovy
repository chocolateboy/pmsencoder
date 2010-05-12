@Typed
package com.chocolatey.pmsencoder

import groovy.util.GroovyTestCase
import org.apache.log4j.xml.DOMConfigurator

import com.chocolatey.pmsencoder.Stash
import com.chocolatey.pmsencoder.Matcher

class CustomConfigTest extends GroovyTestCase {
    Matcher matcher

    void setUp() {
        URL log4jConfig = this.getClass().getResource('/log4j.xml')
        DOMConfigurator.configure(log4jConfig)
        URL pmsencoderConfig = this.getClass().getResource('/pmsencoder.groovy')
        matcher = new Matcher()
        matcher.load(pmsencoderConfig)
    }

    private void assertMatch(
        String uri,
        Stash stash,
        List<String> args,
        List<String> expectedMatches,
        Stash expectedStash,
        List<String> expectedArgs,
        boolean useDefaultArgs = false
    ) {
        List<String> matches = matcher.match(stash, args, useDefaultArgs)

        // println "got matches: $matches"
        // println "want matches: $expectedMatches"
        assert matches == expectedMatches
        // println "got stash: $stash"
        // println "want stash: $expectedStash"
        assert stash == expectedStash
        // println "got args: $args"
        // println "want args: $expectedArgs"
        assert args == expectedArgs
    }

    void testOverrideDefaultArgs() {
        URL customConfig = this.getClass().getResource('/args.groovy')
        matcher.load(customConfig)

        def uri = 'http://www.example.com'
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(uri: uri)

        assertMatch(
            uri,          // URI
            stash,        // stash
            [],           // args
            [],           // expected matches
            want_stash,   // expected stash
            [             // expected args
                '-foo',
                '-bar',
                '-baz',
                '-quux'
            ],
            true          // use default args
        )
    }

    void testReplace() {
        URL customConfig = this.getClass().getResource('/replace.groovy')
        matcher.load(customConfig)

        def uri = 'http://feedproxy.google.com/~r/TEDTalks_video'
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(uri: uri + '/foo/bar.baz')

        assertMatch(
            uri,          // URI
            stash,        // stash
            [],           // args
            [ 'TED' ],    // expected matches
            want_stash,   // expected stash
            [             // expected args
                '-foo',
                'bar',
            ]
        )
    }

    void testAppend() {
        URL customConfig = this.getClass().getResource('/append.groovy')
        matcher.load(customConfig)

        def uri = 'http://www.example.com'
        def stash = new Stash(uri: uri)
        def want_stash = new Stash(uri: uri)

        assertMatch(
            uri,           // URI
            stash,         // stash
            [],            // args
            [ 'Example' ], // expected matches
            want_stash,    // expected stash
            [              // expected args
                '-an',
                'example'
            ]
        )
    }
}
