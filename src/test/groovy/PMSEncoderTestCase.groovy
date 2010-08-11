@Typed
package com.chocolatey.pmsencoder

import groovy.util.GroovyTestCase
import org.apache.log4j.xml.DOMConfigurator

// common test boilerplate
abstract class PMSEncoderTestCase extends GroovyTestCase {
    protected Matcher matcher

    void setUp() {
        def log4jConfig = this.getClass().getResource('/log4j_test.xml')
        def pmsencoderConfig = this.getClass().getResource('/pmsencoder.groovy')

        DOMConfigurator.configure(log4jConfig)
        matcher = new Matcher()
        matcher.load(pmsencoderConfig)
    }

    protected void assertMatch(
        Command command,
        Command expectedCommand,
        List<String> expectedMatches,
        boolean useDefaultArgs = false
    ) {
        List<String> matches = matcher.match(command, useDefaultArgs)

        assert command == expectedCommand : "$command != $expectedCommand"
        assert matches == expectedMatches : "$matches != $expectedMatches"
    }
}
