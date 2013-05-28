package com.chocolatey.pmsencoder

import groovy.util.GroovyTestCase
import mockit.*
import net.pms.configuration.PmsConfiguration
import net.pms.PMS
import org.apache.log4j.xml.DOMConfigurator

import ch.qos.logback.classic.Logger as LogbackLogger
import ch.qos.logback.classic.Level as LogbackLevel
import org.slf4j.LoggerFactory

// there's no point trying to optimize this while we're still using JUnit:
// http://tinyurl.com/6k6z6dj
@groovy.transform.CompileStatic
abstract class PMSEncoderTestCase extends GroovyTestCase {
    protected Matcher matcher
    private PMS pms
    private URL defaultScript

    static {
        // FIXME hack to shut httpclient the hell up
        LogbackLogger tempLogger = LoggerFactory.getLogger('org.apache.http') as LogbackLogger
        tempLogger.setLevel(LogbackLevel.WARN)
        tempLogger = LoggerFactory.getLogger('groovyx.net.http') as LogbackLogger
        tempLogger.setLevel(LogbackLevel.WARN)
    }

    void setUp() {
        def log4jConfig = this.getClass().getResource('/log4j_test.xml')
        DOMConfigurator.configure(log4jConfig)

        defaultScript = this.getClass().getResource('/DEFAULT.groovy')

        new MockUp<PmsConfiguration>() {
            private static final Map<String, Object> PMS_CONFIGURATION = new HashMap<String, Object>()

            static {
                PMS_CONFIGURATION.put('rtmpdump.path', '/usr/bin/rtmpdump')
            }

            @Mock
            public int getNumberOfCpuCores() { 3 }

            @Mock
            public Object getCustomProperty(String key) {
                return PMS_CONFIGURATION.get(key)
            }

            @Mock
            public void setCustomProperty(String key, Object value) {
                PMS_CONFIGURATION.put(key, value)
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

    // allow ActionDelegate methods to be tested without having to do so indirectly through scripts
    public ActionDelegate getAction(Command command = null) {
        return new ActionDelegate(getProfileDelegate())
    }

    protected void assertMatch(Map<String, Object> spec) {
        if (spec['loadDefaultScripts']) {
            matcher.loadDefaultScripts()
        }

        List<URL> scriptURLs

        if (spec['script'] != null) {
            List<Object> scripts

            if (spec['script'] instanceof List) {
                scripts = spec['script'] as List<Object>
            } else {
                scripts = [ spec['script'] ]
            }

            scriptURLs = scripts.collect {
                URL url

                if (it instanceof URL) {
                    // needed to fix a CompileStatic error, but according to
                    // the "documentation", a successful instanceof is meant to
                    // resolve the type (like Kotlin)
                    url = it as URL
                } else {
                    url = this.getClass().getResource(it.toString())
                }

                assert url != null
                return url
            }
        }

        Stash stash

        if (spec.containsKey('stash')) {
            Map<String, Object> map = spec['stash'] as Map<String, Object>
            stash = new Stash(map)
        } else { // uri can be null (not all tests need it)
            String uri = spec['uri']

            if (uri == null) {
                stash = new Stash()
            } else {
                stash = new Stash([ uri: uri ])
            }
        }

        List<String> wantMatches = getValue(spec, 'wantMatches') as List<String>
        List<String> hook = getValue(spec, 'hook') as List<String>
        List<String> downloader = getValue(spec, 'downloader') as List<String>
        List<String> transcoder = getValue(spec, 'transcoder') as List<String>

        def wantStash = getValue(spec, 'wantStash')
        def wantHook = getValue(spec, 'wantHook')
        def wantDownloader = getValue(spec, 'wantDownloader')
        def wantTranscoder = getValue(spec, 'wantTranscoder')

        boolean useDefaultTranscoder = getValue(spec, 'useDefaultTranscoder', false)

        if (scriptURLs != null) {
            scriptURLs.each { URL url ->
                assert url != null
                matcher.load(url)
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
    }
}
