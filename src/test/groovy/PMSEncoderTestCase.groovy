@Typed
package com.chocolatey.pmsencoder

import groovy.util.GroovyTestCase
import mockit.*
import net.pms.configuration.PmsConfiguration
import net.pms.PMS
import org.apache.log4j.xml.DOMConfigurator

abstract class PMSEncoderTestCase extends GroovyTestCase {
    protected Matcher matcher
    protected PMS pms

    void setUp() {
        def log4jConfig = this.getClass().getResource('/log4j_test.xml')
        def pmsencoderConfig = this.getClass().getResource('/pmsencoder.groovy')

        new MockUp<PmsConfiguration>() {
            @Mock
            public int getNumberOfCpuCores() { 3 }
        };

        new MockUp<PMS>() {
            static final PmsConfiguration pmsConfig = new PmsConfiguration()

            @Mock
            public boolean init () { true }

            @Mock
            public static void minimal(String msg) {
                println msg
            }

            @Mock
            public static PmsConfiguration getConfiguration() { pmsConfig }
        };

        pms = PMS.get()

        DOMConfigurator.configure(log4jConfig)
        matcher = new Matcher(pms)
        matcher.load(pmsencoderConfig)
    }

    protected void assertMatch(
        Command command,
        Command expectedCommand,
        List<String> expectedMatches,
        boolean useDefaultArgs = false
    ) {
        matcher.match(command, useDefaultArgs)
        assertEquals(expectedCommand.stash, command.stash)
        assertEquals(expectedCommand.args, command.args)
        assertEquals(expectedMatches, command.matches)
    }
}
