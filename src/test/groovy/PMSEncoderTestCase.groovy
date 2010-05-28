@Typed
package com.chocolatey.pmsencoder

import groovy.util.GroovyTestCase
import org.apache.log4j.xml.DOMConfigurator

// common test boilerplate
abstract class PMSEncoderTestCase extends GroovyTestCase {
    protected Matcher matcher

    void setUp() {
        URL log4jConfig = this.getClass().getResource('/log4j.xml')
        DOMConfigurator.configure(log4jConfig)
        URL pmsencoderConfig = this.getClass().getResource('/pmsencoder.groovy')
        matcher = new Matcher()
        matcher.load(pmsencoderConfig)
    }

    protected void assertMatch(
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
}
