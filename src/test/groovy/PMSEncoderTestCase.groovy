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
        def log4jConfig = this.getClass().getResource('/test_log4j.xml')
        def pmsencoderConfig = this.getClass().getResource('/pmsencoder.groovy')

        new MockUp<PmsConfiguration>() {
            @Mock
            public int getNumberOfCpuCores() { 3 }
        }

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
        }

        pms = PMS.get()

        DOMConfigurator.configure(log4jConfig)
        matcher = new Matcher(pms)
        matcher.load(pmsencoderConfig)
    }

    protected void assertMatch(Map<String, Object> map) {
        URL script

        if (map['script'] != null) {
            if (map['script'] instanceof URL) {
                script = map['script']
            } else {
                script = this.getClass().getResource(map['script'] as String)
            }
        }

        Stash stash
        String uri = map['uri']

        if (uri != null) {
            stash = [ $URI: uri ]
        } else if (map['stash'] != null) {
            stash = map['stash']
        }

        assert stash != null

        List<String> args = map['args'] ?: []
        List<String> hook = map['hook']
        List<String> downloader = map['downloader']
        List<String> transcoder = map['transcoder']

        def wantArgs = map['wantArgs'] ?: args
        def wantStash = map['wantStash'] ?: stash

        List<String> matches = map['matches'] ?: []

        boolean useDefaultArgs = map['useDefaultArgs'] ?: false

        if (script != null) {
            matcher.load(script)
        }

        def command = new Command(stash, args)
        matcher.match(command, useDefaultArgs)

        /*
           XXX

            Groovy(++) bug: strongly-typing the wantStash and wantArgs closures
            results in an exception when the closure contains a String =~ String expression
            (i.e. returns a Matcher):

                java.lang.ClassCastException: java.util.regex.Matcher cannot be cast to java.lang.Boolean

            This contradicts TFM.

            Loosely-typing them as mere Closures works around this.
        */

        if (wantStash instanceof Closure) {
            assert (wantStash as Closure).call(command.stash)
        } else {
            assert command.stash == wantStash
        }

        if (wantArgs instanceof Closure) {
            assert (wantArgs as Closure).call(command.args)
        } else {
            assert command.args == wantArgs
        }

        assert matches == command.matches
        assert hook == command.hook
        assert downloader == command.downloader
        assert transcoder == command.transcoder
    }
}
