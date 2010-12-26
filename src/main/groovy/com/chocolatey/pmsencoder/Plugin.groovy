@Typed
package com.chocolatey.pmsencoder

import static groovy.io.FileType.FILES

import javax.swing.JComponent

import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.Player
import net.pms.external.StartStopListener
import net.pms.formats.Format
import net.pms.PMS

import no.geosoft.cc.io.FileListener
import no.geosoft.cc.io.FileMonitor

import org.apache.log4j.xml.DOMConfigurator

public class Plugin implements StartStopListener, FileListener {
    private static final String VERSION = '1.2.3'
    private static final String DEFAULT_SCRIPT_DIRECTORY = 'pmsencoder'
    private static final String LOG_CONFIG = 'pmsencoder.log.config'
    private static final String SCRIPT_DIRECTORY = 'pmsencoder.script.directory'
    private static final String SCRIPT_POLL = 'pmsencoder.script.poll'
    // 1 second is flaky - it results in overlapping file change events
    private static final int MIN_SCRIPT_POLL_INTERVAL = 2
    private Engine pmsencoder
    private FileMonitor fileMonitor
    private File scriptDirectory
    private long scriptPollInterval
    private Matcher matcher
    private PmsConfiguration configuration
    private PMS pms
    private URL defaultScript

    public Plugin() {
        info('initializing PMSEncoder ' + VERSION)
        pms = PMS.get()
        configuration = PMS.getConfiguration()
        defaultScript = this.getClass().getResource('/pmsencoder.groovy')

        // get optional overrides from PMS.conf
        def customLogConfigPath = (configuration.getCustomProperty(LOG_CONFIG) as String)
        def candidateScriptDirectory = (configuration.getCustomProperty(SCRIPT_DIRECTORY) as String)
        def candidateScriptPollInterval = (configuration.getCustomProperty(SCRIPT_POLL) as int) ?: 0

        if (candidateScriptDirectory != null) {
            def candidateScriptDirectoryFile = new File(candidateScriptDirectory)

            if (directoryExists(candidateScriptDirectoryFile)) {
                scriptDirectory = candidateScriptDirectoryFile.getAbsoluteFile()
            } else {
                def absPath = candidateScriptDirectoryFile.getAbsolutePath()
                error("invalid path for script directory ($absPath): no such directory", null)
            }
        } else {
            def candidateScriptDirectoryFile = new File(DEFAULT_SCRIPT_DIRECTORY)

            if (directoryExists(candidateScriptDirectoryFile)) {
                scriptDirectory = candidateScriptDirectoryFile.getAbsoluteFile()
            }
        }

        if (scriptDirectory != null) {
            info("script directory: $scriptDirectory")

            if (candidateScriptPollInterval > 0) {
                if (candidateScriptPollInterval < MIN_SCRIPT_POLL_INTERVAL) {
                    candidateScriptPollInterval = MIN_SCRIPT_POLL_INTERVAL
                }
                info("setting polling interval to $candidateScriptPollInterval seconds")
                scriptPollInterval = (candidateScriptPollInterval * 1000) as long
                monitorScriptDirectory()
            }
        }

        // set up log4j
        def customLogConfig

        if (customLogConfigPath != null) {
            def customLogConfigFile = new File(customLogConfigPath)

            if (fileExists(customLogConfigFile)) {
                customLogConfig = customLogConfigFile.getAbsolutePath()
            } else {
                def absPath = customLogConfigFile.getAbsolutePath()
                error("invalid path for log4j config file ($absPath): no such file", null)
            }
        }

        Closure loadDefaultLogConfig = {
            // XXX squashed bug - don't call this log4j.xml, as, by default,
            // log4j attempts to load log4j.properties and log4j.xml automatically
            def defaultLogConfig = this.getClass().getResource('/default_log4j.xml')
            info("loading built-in log4j config file: $defaultLogConfig")

            try {
                DOMConfigurator.configure(defaultLogConfig)
            } catch (Exception e) {
                error("error loading built-in log4j config file ($defaultLogConfig)", e)
            }
        }

        if (customLogConfig != null) {
            info("loading custom log4j config file: $customLogConfig")

            try {
                DOMConfigurator.configure(customLogConfig)
            } catch (Exception e) {
                error("error loading log4j config file ($customLogConfig)", e)
                loadDefaultLogConfig()
            }
        } else {
            loadDefaultLogConfig()
        }

        // make sure we have a matcher before we create the transcoder
        createMatcher()

        // initialize the transcoding Engine
        pmsencoder = new Engine(configuration, this)

        /*
         * FIXME: don't assume the position is fixed
         * short term: find and replace *if it exists*
         * long term: patch PMS to allow plugins to register engines a) separately and b) cleanly
         * */
        def extensions = pms.getExtensions()
        extensions.set(0, new WEB())
        registerPlayer(pmsencoder)
    }

    private boolean fileExists(File file) {
        (file != null) && file.exists() && file.isFile()
    }

    private boolean directoryExists(File file) {
        (file != null) && file.exists() && file.isDirectory()
    }

    private void info(String message) {
        PMS.minimal("PMSEncoder: $message")
    }

    private void error(String message, Exception e) {
        PMS.error("PMSEncoder: $message", e)
    }

    private void monitorScriptDirectory() {
        fileMonitor = new FileMonitor(scriptPollInterval)
        fileMonitor.addFile(scriptDirectory)
        fileMonitor.addListener(this)
    }

    public void fileChanged(File file) {
        info("$file has changed; reloading scripts")
        createMatcher()
    }

    private synchronized void createMatcher() {
        matcher = new Matcher(pms)

        try {
            loadScripts()
        } catch (Exception e) {
            error('error loading scripts', e)
        }
    }

    private void loadScripts() {
        loadScript(defaultScript)

        if (directoryExists(scriptDirectory)) {
            info("loading scripts from: $scriptDirectory")

            scriptDirectory.eachFileRecurse(FILES) { File file ->
                if (file.getName().endsWith('.groovy')) {
                    loadScript(file)
                }
            }
        }
    }

    private void loadScript(URL script) {
        info("loading built-in script: $script")
        try {
            matcher.load(script)
        } catch (Exception e) {
            error("can't load built-in script: $script", e)
        }
    }

    private void loadScript(File script) {
        info("loading user script: $script")
        try {
            matcher.load(script)
        } catch (Exception e) {
            error("can't load user script: $script", e)
        }
    }

    private void registerPlayer(Engine pmsencoder) {
        try {
            def pmsRegisterPlayer = pms.getClass().getDeclaredMethod('registerPlayer', Player.class)
            pmsRegisterPlayer.setAccessible(true)
            pmsRegisterPlayer.invoke(pms, pmsencoder)
        } catch (Exception e) {
            info('error calling PMS.registerPlayer: ' + e)
        }
    }

    public boolean match(Command command) {
        matcher.match(command)
    }

    @Override
    public JComponent config() {
        return null
    }

    @Override
    public String name() {
        return 'PMSEncoder plugin for PS3 Media Server'
    }

    @Override
    public void nowPlaying(DLNAMediaInfo media, DLNAResource resource) {

    }

    @Override
    public void donePlaying(DLNAMediaInfo media, DLNAResource resource) {

    }

    @Override
    public void shutdown () {
        if (fileMonitor != null) {
            fileMonitor.stop()
        }
    }
}
