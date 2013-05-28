package com.chocolatey.pmsencoder

import static com.chocolatey.pmsencoder.Util.guard

import static groovy.io.FileType.FILES
import groovy.swing.SwingBuilder // TODO

import javax.swing.JComponent
import javax.swing.JFrame

import net.pms.configuration.PmsConfiguration
import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.Player
import net.pms.encoders.PlayerFactory
import net.pms.external.ExternalListener
import net.pms.external.FinalizeTranscoderArgsListener
import net.pms.io.OutputParams
import net.pms.logging.DebugLogPathDefiner
import net.pms.PMS

import no.geosoft.cc.io.FileListener
import no.geosoft.cc.io.FileMonitor

import org.apache.log4j.Logger as Log4jLogger
import org.apache.log4j.xml.DOMConfigurator

import ch.qos.logback.classic.Logger as LogbackLogger
import ch.qos.logback.classic.Level as LogbackLevel
import org.slf4j.LoggerFactory

@groovy.transform.CompileStatic
public class Plugin implements ExternalListener, FinalizeTranscoderArgsListener, FileListener {
    private static final String VERSION = '2.0.0-SNAPSHOT'
    private static final String DEFAULT_SCRIPT_DIRECTORY = 'pmsencoder'
    private static final String LOG_CONFIG = 'pmsencoder.log.config'
    private static final String LOG_DIRECTORY = 'pmsencoder.log.directory'
    private static final String SCRIPT_DIRECTORY = 'pmsencoder.script.directory'
    private static final String SCRIPT_POLL = 'pmsencoder.script.poll'
    // 1 second is flaky - it results in overlapping file change events
    private static final int MIN_SCRIPT_POLL_INTERVAL = 2
    private static Log4jLogger pmsencoderLogger
    private static final LogbackLogger PMS_LOGGER = LoggerFactory.getLogger(this.class) as LogbackLogger

    private PMSEncoder pmsencoder
    private FileMonitor fileMonitor
    private File scriptDirectory
    private long scriptPollInterval
    private Matcher matcher
    private PmsConfiguration configuration
    private PMS pms
    private Object lock = new Object()

    public Plugin() {
        info('initializing PMSEncoder ' + VERSION)
        pms = PMS.get()
        configuration = PMS.getConfiguration()

        // get optional overrides from PMS.conf
        String customLogConfigPath = configuration.getCustomProperty(LOG_CONFIG)
        String customLogDirectory = configuration.getCustomProperty(LOG_DIRECTORY)
        String candidateScriptDirectory = configuration.getCustomProperty(SCRIPT_DIRECTORY)

        /*
           XXX: When Groovy breaks down...

           long-windedness is required here to ensure that a string is correctly converted to
           an Integer. in a previous incarnation:

                pmsencoder.script.poll = 2

            resulted in a poll interval of 50 (i.e. the ASCII value of the character "2")
        */

        // cast the expression to the type of the default value (int) and return the default value
        // (0) if an exception (in this case a java.lang.NumberFormatException) is thrown
        String scriptPollString = configuration.getCustomProperty(SCRIPT_POLL)
        // changing this "int" to "def" produces a Verify error (see TODO.groovy)
        int candidateScriptPollInterval = guard (0) { scriptPollString.toInteger() }

        // handle scripts
        if (candidateScriptDirectory) {
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

        if (scriptDirectory) {
            info("script directory: $scriptDirectory")

            if (candidateScriptPollInterval > 0) {
                if (candidateScriptPollInterval < MIN_SCRIPT_POLL_INTERVAL) {
                    candidateScriptPollInterval = MIN_SCRIPT_POLL_INTERVAL
                }
                info("setting polling interval to $candidateScriptPollInterval seconds")
                scriptPollInterval = candidateScriptPollInterval * 1000
                monitorScriptDirectory()
            }
        }

        // set up log4j

        // set the log path as a system property so that it can be used in log4j_default.xml
        // 1) system property
        // 2) PMS.conf option
        // 3) same directory as the debug.log
        if (!System.getProperty(LOG_DIRECTORY)) {
            System.setProperty(LOG_DIRECTORY, customLogDirectory ?: (new DebugLogPathDefiner()).getPropertyValue())
        }

        info("log directory: " + System.getProperty(LOG_DIRECTORY))

        def customLogConfig

        if (customLogConfigPath) {
            def customLogConfigFile = new File(customLogConfigPath)

            if (fileExists(customLogConfigFile)) {
                customLogConfig = customLogConfigFile.getAbsolutePath()
            } else {
                def absPath = customLogConfigFile.getAbsolutePath()
                error("invalid path for log4j config file ($absPath): no such file", null)
            }
        }

        // load log4j config file
        if (customLogConfig) {
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

        // FIXME hack to shut httpclient the hell up
        LogbackLogger tempLogger = LoggerFactory.getLogger('org.apache.http') as LogbackLogger
        tempLogger.setLevel(LogbackLevel.WARN)
        tempLogger = LoggerFactory.getLogger('groovyx.net.http') as LogbackLogger
        tempLogger.setLevel(LogbackLevel.WARN)

        // now we've loaded the logger config file we can initialise the pmsencoder.log logger for this class
        pmsencoderLogger = Log4jLogger.getLogger(this.getClass().getName())

        // make sure we have a matcher before we create the transcoding engine
        createMatcher()

        // initialize the transcoding engine
        pmsencoder = new PMSEncoder(configuration, this)

        /*
         * FIXME: don't assume the position is fixed
         * short term: find and replace *if it exists*
         * long term: patch PMS to allow plugins to register engines a) separately and b) cleanly
         * */
        def extensions = pms.getExtensions()
        extensions.set(0, new WEB())
        PlayerFactory.registerPlayer(pmsencoder)

        // add to the engines list
        enable()
    }

    // make sure "pmsencoder" is in the list of engines
    private void enable() {
        def engines = configuration.getEnginesAsList(pms.getRegistry())
        def id = pmsencoder.id()
        def index = engines.indexOf(id)

        info("checking engine list: $engines")

        if (index != -1) {
            info('already enabled')
        } else {
            def newEngines = new ArrayList<String>(engines)
            newEngines.add(0, id)
            configuration.setEnginesAsList(newEngines)
            info("added engine: $newEngines")
        }
    }

    private void loadDefaultLogConfig() {
        // XXX squashed bug - don't call this log4j.xml, as, by default,
        // log4j attempts to load log4j.properties and log4j.xml automatically
        def defaultLogConfig = this.getClass().getResource('/log4j_default.xml')
        info("loading built-in log4j config file: $defaultLogConfig")

        try {
            DOMConfigurator.configure(defaultLogConfig)
        } catch (Exception e) {
            error("error loading built-in log4j config file ($defaultLogConfig)", e)
        }
    }

    private boolean fileExists(File file) {
        (file != null) && file.exists() && file.isFile()
    }

    private boolean directoryExists(File file) {
        (file != null) && file.exists() && file.isDirectory()
    }

    private void info(String message) {
        PMS_LOGGER.info("PMSEncoder: $message")
    }

    private void error(String message, Throwable e) {
        PMS_LOGGER.error("PMSEncoder: $message", e)
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

    private void createMatcher() {
        synchronized (lock) {
            matcher = new Matcher(pms)

            try {
                matcher.loadDefaultScripts()

                if (directoryExists(scriptDirectory)) {
                    matcher.loadUserScripts(scriptDirectory)
                }
            } catch (Exception e) {
                error('error loading scripts', e)
            }
        }
    }

    public boolean match(Command command) {
        def stash = command.stash
        def originalURI = command.getVarAsString('uri')

        boolean matched // Groovy++ type inference fail

        pmsencoderLogger.info('invoking matcher for: ' + originalURI)

        try {
            matched = matcher.match(command)
        } catch (Throwable e) {
            pmsencoderLogger.error('match error: ' + e)
            error('match error', e)
        }

        def matches = command.getMatches()
        def nMatches = matches.size()

        if (nMatches == 0) {
            pmsencoderLogger.info('0 matches for: ' + originalURI)
        } else if (nMatches == 1) {
            pmsencoderLogger.info('1 match (' + matches + ') for: ' + originalURI)
        } else {
            pmsencoderLogger.info(nMatches + ' matches (' + matches + ') for: ' + originalURI)
        }

        return matched
    }

    @Override
    public List<String> finalizeTranscoderArgs(
        Player player,
        String filename,
        DLNAResource dlna,
        DLNAMediaInfo media,
        OutputParams params,
        List<String> cmdList
    ) {
        def uri = new File(filename).toURI().toString() // file:// URI
        def stash = new Stash([
            engine: player.id(),
            filename: filename,
            uri: uri
        ])

        def command = new Command(stash, cmdList)

        command.setDlna(dlna)
        command.setMedia(media)
        command.setParams(params)
        command.setPlayer(player)

        def matched = match(command)

        if (matched) {
            cmdList = command.transcoder
        }

        return cmdList
    }

    @Override
    public JComponent config() {
        return null
    }

    @Override
    public String name() {
        return 'PMSEncoder'
    }

    @Override
    public void shutdown () {
        if (fileMonitor != null) {
            fileMonitor.stop()
        }
    }
}
