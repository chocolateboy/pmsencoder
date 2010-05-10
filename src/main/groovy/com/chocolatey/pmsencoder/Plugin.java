package com.chocolatey.pmsencoder;

import java.net.URL;
import java.lang.reflect.Method;
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
    private Matcher matcher;
    private URL log4jConfig;
    private URL pmsencoderConfig;
    private static final String PMSENCODER_CONFIG_FILE_PATH = "pmsencoder.config_file"; // XXX not used yet
    private PmsConfiguration configuration;
    private Engine pmsencoder;
    private String customConfigFile; // XXX not used yet
    private PMS pms;
    private ArrayList<Format> extensions;
    private static final String VERSION = "1.0.0";

    public Plugin() {
        PMS.minimal("initializing PMSEncoder " + VERSION);

        // set up log4j
        log4jConfig = this.getClass().getResource("/log4j.xml");
        PMS.minimal("log4j config file: " + log4jConfig);
        DOMConfigurator.configure(log4jConfig);

        // load default PMSEncoder config file
        pmsencoderConfig = this.getClass().getResource("/pmsencoder.groovy");
        PMS.minimal("PMSEncoder config file: " + pmsencoderConfig);
        matcher = new Matcher(pmsencoderConfig);

        // initialize the PMSEncoder object that launches the transcode
        configuration = PMS.getConfiguration();
        pmsencoder = new Engine(configuration, matcher);
        customConfigFile = (String)configuration.getCustomProperty(PMSENCODER_CONFIG_FILE_PATH);

        if (customConfigFile != null) {
            PMS.minimal("custom config file defined: " + customConfigFile);
        }

        pms = PMS.get();
        extensions = pms.getExtensions();

        /*
         * FIXME: don't assume the position is fixed
         * short term: find and replace *if it exists*
         * long term: patch PMS to allow plugins to register engines a) separately and b) cleanly
         * */
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
