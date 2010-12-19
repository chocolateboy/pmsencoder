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

import org.apache.log4j.xml.DOMConfigurator

public class Plugin implements StartStopListener {
    private static final String VERSION = '1.2.0'
    private static final String PMSENCODER_SCRIPT_DIRECTORY = 'pmsencoder_script_directory'
    private static final String PMSENCODER_LOG_CONFIG = 'pmsencoder_log_config'

    private PmsConfiguration configuration
    private PMS pms

    public Plugin() {
        PMS.minimal('initializing PMSEncoder ' + VERSION)
        pms = PMS.get()
        configuration = PMS.getConfiguration()

        // get optional overrides from PMS.conf
        def customLogConfigPath = (configuration.getCustomProperty(PMSENCODER_LOG_CONFIG) as String)
        def scriptDirectory = (configuration.getCustomProperty(PMSENCODER_SCRIPT_DIRECTORY) as String)

        // set up log4j
        def customLogConfig

        if (customLogConfigPath != null) {
            def customLogConfigFile = new File(customLogConfigPath)

            if (customLogConfigFile.exists()) {
                customLogConfig = customLogConfigPath
            } else {
                PMS.error("invalid path for log4j config file ($customLogConfigPath): file doesn't exist", null)
            }
        }

        Closure loadDefaultLogConfig = {
            PMS.minimal('loading built-in log4j config file')
            def defaultLogConfig = this.getClass().getResource('/log4j.xml')

            try {
                DOMConfigurator.configure(defaultLogConfig)
            } catch (Exception e) {
                PMS.error("error loading built-in log4j config file ($defaultLogConfig)", e)
            }
        }

        if (customLogConfig != null) {
            PMS.minimal("loading custom log4j config file: $customLogConfig")

            try {
                DOMConfigurator.configure(customLogConfig)
            } catch (Exception e) {
                PMS.error("error loading log4j config file ($customLogConfig)", e)
                loadDefaultLogConfig()
            }
        } else {
            loadDefaultLogConfig()
        }

        // load default script and user scripts
        def matcher = new Matcher(pms)

        try {
            loadScripts(matcher, scriptDirectory)
        } catch (Exception e) {
            PMS.error('error loading scripts', e)
        }

        // initialize the transcoding Engine
        def pmsencoder = new Engine(configuration, matcher)

        /*
         * FIXME: don't assume the position is fixed
         * short term: find and replace *if it exists*
         * long term: patch PMS to allow plugins to register engines a) separately and b) cleanly
         * */
        def extensions = pms.getExtensions()
        extensions.set(0, new WEB())
        registerPlayer(pmsencoder)
    }

    private void registerPlayer(Engine pmsencoder) {
        try {
            def pmsRegisterPlayer = pms.getClass().getDeclaredMethod('registerPlayer', Player.class)
            pmsRegisterPlayer.setAccessible(true)
            pmsRegisterPlayer.invoke(pms, pmsencoder)
        } catch (Throwable e) {
            PMS.minimal('error calling PMS.registerPlayer: ' + e)
        }
    }

    private void loadScripts(Matcher matcher, String scriptDirectory) {
        def currentDirectory = new File('').getAbsolutePath()
        def defaultScript = this.getClass().getResource('/pmsencoder.groovy')

        loadScript(matcher, defaultScript)

        if (scriptDirectory != null) {
            PMS.minimal("loading scripts from: $scriptDirectory")

            new File(scriptDirectory).eachFileRecurse(FILES) { File file ->
                if (file.getName().endsWith('.groovy')) {
                    loadScript(matcher, file)
                }
            }
        } else {
            // loadScript checks for the file's existence
            loadScript(matcher, new File('pmsencoder.groovy'))
        }
    }

    private void loadScript(Matcher matcher, Object script) { // script is either a URL or a File
        try {
            if (script instanceof URL) {
                PMS.minimal('loading built-in script')
                matcher.load(script as URL)
            } else {
                def file = script as File

                if (file.exists()) {
                    PMS.minimal('loading script: ' + file)
                    matcher.load(file)
                }
            }
        } catch (Throwable e) {
            PMS.error("can't load PMSEncoder script: " + script, e)
        }
    }

    @Override
    public String name() {
        return 'PMSEncoder plugin for PS3 Media Server'
    }

    @Override
    public JComponent config() { // no config GUI (though Griffon would make this bearable)
        return null
    }

    @Override
    public void nowPlaying(DLNAMediaInfo media, DLNAResource resource) {

    }

    @Override
    public void donePlaying(DLNAMediaInfo media, DLNAResource resource) {

    }

    @Override
    public void shutdown () {
        // nothing to do
    }
}
