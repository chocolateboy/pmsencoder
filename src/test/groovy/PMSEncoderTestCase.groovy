@Typed
package com.chocolatey.pmsencoder

import groovy.util.GroovyTestCase
import mockit.*
import net.pms.configuration.PmsConfiguration
import net.pms.PMS
import org.apache.log4j.xml.DOMConfigurator

import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

// there's no point trying to optimize this while we're still using JUnit:
// http://tinyurl.com/6k6z6dj
abstract class PMSEncoderTestCase extends GroovyTestCase {
    protected Matcher matcher
    private PMS pms
    private URL defaultScript

    static {
        // FIXME hack to shut httpclient the hell up
        Logger tempLogger = LoggerFactory.getLogger("org.apache.http");
        tempLogger.setLevel(ch.qos.logback.classic.Level.WARN)
        tempLogger = LoggerFactory.getLogger("groovyx.net.http");
        tempLogger.setLevel(ch.qos.logback.classic.Level.WARN)
    }

    void setUp() {
        def log4jConfig = this.getClass().getResource('/log4j_test.xml')
        DOMConfigurator.configure(log4jConfig)

        defaultScript = this.getClass().getResource('/DEFAULT.groovy')

        new MockUp<PmsConfiguration>() {
            @Mock
            public int getNumberOfCpuCores() { 3 }

            @Mock
            public Object getCustomProperty(String key) {
                if (key == 'rtmpdump.path') {
                    return '/usr/bin/rtmpdump'
                } else if (key == 'ffmpeg.http-headers') {
                    return "false"
                } else {
                    return null
                }
            }
        }

        new MockUp<PMS>() {
            static final PmsConfiguration pmsConfig = new PmsConfiguration()

            @Mock
            public boolean init () { true }

            @Mock
            public static void info(String msg) {
                println msg
            }

            @Mock
            public static PmsConfiguration getConfiguration() { pmsConfig }
        }

        pms = PMS.get()
        matcher = new Matcher(pms)
    }

    private Object getValue(Map<String, Object> map, String key, Object defaultValue = null) {
        if (map.containsKey(key)) {
            return map[key]
        } else {
            return defaultValue
        }
    }

    // allow ProfileDelegate methods to be tested without having to do so indirectly through scripts
    public ProfileDelegate getProfileDelegate(Command command = null) {
        return new ProfileDelegate(matcher, command ?: new Command())
    }

    protected void assertMatch(Map<String, Object> spec) {
        if (spec['loadDefaultScripts']) {
            matcher.loadDefaultScripts()
        }

        List<URL> scripts

        if (spec['script'] != null) {
            if (!(spec['script'] instanceof List)) {
                spec['script'] = [ spec['script'] ]
            }

            scripts = spec['script'].collect {
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

        if (spec.containsKey('stash')) {
            Map<String, String> map = spec['stash']
            stash = new Stash(map)
        } else { // uri can be null (not all tests need it)
            String uri = spec['uri']
            stash = new Stash([ $URI: uri ])
        }

        List<String> wantMatches = getValue(spec, 'wantMatches')
        List<String> hook = getValue(spec, 'hook')
        List<String> downloader = getValue(spec, 'downloader')
        List<String> transcoder = getValue(spec, 'transcoder')
        List<String> output = getValue(spec, 'output')

        def wantStash = getValue(spec, 'wantStash')
        def wantHook = getValue(spec, 'wantHook')
        def wantDownloader = getValue(spec, 'wantDownloader')
        def wantTranscoder = getValue(spec, 'wantTranscoder')
        def wantOutput = getValue(spec, 'wantOutput')

        boolean useDefaultTranscoder = getValue(spec, 'useDefaultTranscoder', false)

        if (scripts != null) {
            scripts.each {
                assert it != null
                matcher.load(it)
            }
        }

        def command

        if (transcoder != null) {
            if (stash != null) {
                command = new Command(stash, transcoder)
            } else {
                command = new Command(transcoder)
            }
        } else if (stash != null) {
            command = new Command(stash)
        } else {
            command = new Command()
        }

        if (hook != null) {
            command.hook = hook
        }

        if (downloader != null) {
            command.downloader = downloader
        }

        if (transcoder != null) {
            command.transcoder = transcoder
        }

        if (output != null) {
            command.output = output
        }

        matcher.match(command, useDefaultTranscoder)

        if (wantMatches != null) {
            assert command.matches == wantMatches
        }

        /*
           XXX

            Groovy(++) bug: strongly-typing the wantStash and wantTranscoder closures
            results in an exception when the closure contains a String =~ String expression
            (i.e. returns a Matcher):

                java.lang.ClassCastException: java.util.regex.Matcher cannot be cast to java.lang.Boolean

            This contradicts TFM.

            Loosely-typing them as mere Closures works around this.
        */

        if (wantStash != null) {
            if (wantStash instanceof Closure) {
                assert (wantStash as Closure).call(command.stash)
            } else {
                assert command.stash == wantStash
            }
        }

        if (wantHook != null) {
            if (wantHook instanceof Closure) {
                assert (wantHook as Closure).call(command.hook)
            } else {
                assert command.hook == wantHook
            }
        }

        if (wantDownloader != null) {
            if (wantDownloader instanceof Closure) {
                assert (wantDownloader as Closure).call(command.downloader)
            } else {
                assert command.downloader == wantDownloader
            }
        }

        if (wantTranscoder != null) {
            if (wantTranscoder instanceof Closure) {
                assert (wantTranscoder as Closure).call(command.transcoder)
            } else {
                assert command.transcoder == wantTranscoder
            }
        }

        if (wantOutput != null) {
            if (wantOutput instanceof Closure) {
                assert (wantOutput as Closure).call(command.output)
            } else {
                assert command.output == wantOutput
            }
        }
    }
}
