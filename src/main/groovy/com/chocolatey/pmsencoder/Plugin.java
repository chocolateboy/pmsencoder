package com.chocolatey.pmsencoder;

import java.lang.NoSuchMethodException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.swing.JComponent;

import com.chocolatey.pmsencoder.Matcher;
import com.chocolatey.pmsencoder.Engine;
import com.chocolatey.pmsencoder.WEB;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.external.StartStopListener;
import net.pms.formats.Format;
import net.pms.PMS;
import net.pms.newgui.TrTab2;

import org.apache.log4j.xml.DOMConfigurator;

class Plugin implements StartStopListener {
    private Matcher matcher;
    private String log4jConfig;
    private String pmsencoderConfig;
    private static final String PMSENCODER_CONFIG_FILE_PATH = "pmsencoder.config_file"; // XXX not used yet
    private PmsConfiguration configuration;
    private Engine pmsencoder;
    private String customConfigFile; // XXX not used yet
    private PMS pms;
    private ArrayList<Format> extensions;

    public Plugin() {
	PMS.minimal("initializing PMSEncoder");

	// set up log4j
	log4jConfig = this.getClass().getResource("/log4j.xml").getFile();
	PMS.minimal("log4j config file: " + log4jConfig);
	DOMConfigurator.configure(log4jConfig);

	// load default PMSEncoder config file
	pmsencoderConfig = this.getClass().getResource("/pmsencoder.groovy").getFile();
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
	registerPlayer();
	extensions.set(0, new WEB());
    }

    private void registerPlayer() {
	try {
	    Method pmsRegisterPlayer = pmsencoder.getClass().getDeclaredMethod("registerPlayer", pmsencoder.getClass());
	    pmsRegisterPlayer.setAccessible(true);
	    pmsRegisterPlayer.invoke(pms);
	} catch (Throwable e) {
	    PMS.minimal("error calling PMS.registerPlayer" + e);
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
	PMS.minimal("PMSEncoder: now playing");
    }

    @Override
    public void donePlaying(DLNAMediaInfo media, DLNAResource resource) {
	PMS.minimal("PMSEncoder: done playing");
    }

    @Override
    public void shutdown () {
	// nothing to do
    }
}
