@Typed
package com.chocolatey.pmsencoder

import groovy.util.GroovyTestCase
import mockit.*
import net.pms.configuration.PmsConfiguration
import net.pms.PMS
import org.apache.log4j.xml.DOMConfigurator

// there's no point trying to optimize this while we're still using JUnit:
// http://tinyurl.com/6k6z6dj
abstract class PMSEncoderTestCase extends GroovyTestCase {
    protected Matcher matcher
    private PMS pms
    private URL defaultScript

    void setUp() {
        def log4jConfig = this.getClass().getResource('/test_log4j.xml')
        DOMConfigurator.configure(log4jConfig)

        defaultScript = this.getClass().getResource('/DEFAULT.groovy')

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
        matcher = new Matcher(pms)
    }

    protected void assertMatch(Map<String, Object> map) {
        if (map['loadDefaultScripts']) {
            matcher.loadDefaultScripts()
        }

        List<URL> scripts

        if (map['script'] != null) {
            if (!(map['script'] instanceof List)) {
                map['script'] = [ map['script'] ]
            }

            scripts = map['script'].collect {
                def url

                if (it instanceof URL) {
                    url = it
                } else {
                    url = this.getClass().getResource(it as String)
                }

                assert url != null
                return url
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

        List<String> output = map['output']
        List<String> downloader = map['downloader']
        List<String> transcoder = map.containsKey('transcoder') ? map['transcoder'] : []

        def wantStash = map.containsKey('wantStash') ? map['wantStash'] : stash
        def wantHook = map['wantHook']
        def wantDownloader = map['wantDownloader']
        def wantTranscoder = map.containsKey('wantTranscoder') ? map['wantTranscoder'] : transcoder
        def testOutput = map.containsKey('wantOutput')
        def wantOutput = map['wantOutput']

        List<String> matches = map.containsKey('matches') ? map['matches'] : []

        boolean useDefaultTranscoder = map.containsKey('useDefaultTranscoder') ? map['useDefaultTranscoder'] : false

        if (scripts != null) {
            scripts.each {
                assert it != null
                matcher.load(it)
            }
        }

        def command = new Command(stash, transcoder)

        if (downloader != null) {
            command.downloader = downloader
        }

        if (output != null) {
            command.output = output
        }

        matcher.match(command, useDefaultTranscoder)

        assert matches == command.matches

        /*
           XXX

            Groovy(++) bug: strongly-typing the wantStash and wantTranscoder closures
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

        if (wantHook instanceof Closure) {
            assert (wantHook as Closure).call(command.hook)
        } else {
            assert command.hook == wantHook
        }

        if (wantDownloader instanceof Closure) {
            assert (wantDownloader as Closure).call(command.downloader)
        } else {
            assert command.downloader == wantDownloader
        }

        if (wantTranscoder instanceof Closure) {
            assert (wantTranscoder as Closure).call(command.transcoder)
        } else {
            assert command.transcoder == wantTranscoder
        }

        if (testOutput) {
            if (wantOutput instanceof Closure) {
                assert (wantOutput as Closure).call(command.output)
            } else {
                assert command.output == wantOutput
            }
        }
    }
}
