package com.chocolatey.pmsencoder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JComponent;

import com.chocolatey.pmsencoder.Matcher;
import com.chocolatey.pmsencoder.Engine;
import com.chocolatey.pmsencoder.WEB;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.encoders.Player;
import net.pms.external.StartStopListener;
import net.pms.formats.Format;
import net.pms.PMS;

import org.apache.log4j.xml.DOMConfigurator;

public class Plugin implements StartStopListener {
    private static final String VERSION = "1.2.0";
    private static final String PMSENCODER_CONFIG_FILE_PATH = "pmsencoder.config_file";
    private PmsConfiguration configuration;
    private Engine pmsencoder;
    private Matcher matcher;
    private PMS pms;
    private String currentDirectory;

    public Plugin() {
        PMS.minimal("initializing PMSEncoder " + VERSION);
        pms = PMS.get();
        currentDirectory = new File("").getAbsolutePath();

        // set up log4j
        URL log4jConfig = this.getClass().getResource("/log4j.xml");
        PMS.minimal("log4j config file: " + log4jConfig);
        DOMConfigurator.configure(log4jConfig);

        // initialize the Engine object that launches the transcode
        configuration = PMS.getConfiguration();

        // load PMSEncoder config file(s)
        matcher = new Matcher(pms);
        loadConfig();
        pmsencoder = new Engine(configuration, matcher);

        /*
         * FIXME: don't assume the position is fixed
         * short term: find and replace *if it exists*
         * long term: patch PMS to allow plugins to register engines a) separately and b) cleanly
         * */
        ArrayList<Format> extensions = pms.getExtensions();
        extensions.set(0, new WEB());
        registerPlayer();
    }

    private void registerPlayer() {
        try {
            Method pmsRegisterPlayer = pms.getClass().getDeclaredMethod("registerPlayer", Player.class);
            pmsRegisterPlayer.setAccessible(true);
            pmsRegisterPlayer.invoke(pms, pmsencoder);
        } catch (Throwable e) {
            PMS.minimal("error calling PMS.registerPlayer: " + e);
        }
    }

    private boolean loadConfig(Object config) { // config is either a URL or a String
        boolean loaded = true;

        try {
            if (config instanceof URL) {
                PMS.minimal("loading built-in PMSEncoder config file: " + config);
                matcher.load((URL)config);
            } else {
                File configFile = new File((String)config);

                if (configFile.exists()) {
                    PMS.minimal("loading custom PMSEncoder config file: " + config);
                    matcher.load(configFile);
                } else {
                    loaded = false;
                }
            }
        } catch (Throwable e) {
            PMS.error("can't load PMSEncoder config file: " + config, e);
            loaded = false;
        }

        return loaded;
    }

    private void loadConfig() {
        URL pmsencoderConfig = this.getClass().getResource("/pmsencoder.groovy");
        loadConfig(pmsencoderConfig);
        String customConfigPath = (String)configuration.getCustomProperty(PMSENCODER_CONFIG_FILE_PATH);

        if (customConfigPath != null) {
            // TODO: document this
            PMS.minimal("custom PMSEncoder config path defined: " + customConfigPath);
            loadConfig(customConfigPath);
        } else {
            PMS.minimal("checking for a custom PMSEncoder config file in " + currentDirectory);

            if (!loadConfig("pmsencoder.groovy")) {
               loadConfig("pmsencoder.conf");
            }
        }
    }

    @Override
    public String name() {
        return "PMSEncoder plugin for PS3 Media Server";
    }

    @Override
    public JComponent config() { // no config GUI (though Griffon would make this bearable)
        return null;
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
