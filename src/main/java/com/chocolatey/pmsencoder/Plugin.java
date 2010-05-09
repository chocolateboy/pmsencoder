package com.chocolatey.pmsencoder;

import javax.swing.JComponent;

import com.chocolatey.pmsencoder.Matcher;
import com.chocolatey.pmsencoder.PMSEncoder;
import com.chocolatey.pmsencoder.WEB;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.external.StartStopListener;
import net.pms.formats.Format;
import net.pms.PMS;

class Plugin extends StartStopListener {
    private Matcher matcher;
    private String log4jConfig;
    private String pmsencoderConfig;
    private static final String PMSENCODER_CONFIG_FILE_PATH = "pmsencoder.config_file"; // XXX not used yet
    private PmsConfiguration configuration;
    private PMSEncoder pmsencoder;
    private String customConfigFile; // XXX not used yet
    private PMS pms;
    private ArrayList<Format> extensions;

    public Plugin() {
	PMS.minimal("initializing PMSEncoder");

	// set up log4j
	log4jConfig = getClass().getResource('/log4j.xml').getFile();
	PMS.minimal("log4j config file: " + log4jConfig);
	DOMConfigurator.configure(log4jConfig);

	// load default PMSEncoder config file
	pmsencoderConfig = getClass().getResource('/pmsencoder.groovy').getFile();
	PMS.minimal("PMSEncoder config file: " + pmsencoderConfig);
	matcher = new Matcher(pmsencoderConfig);

	// initialize the PMSEncoder object that launches the transcode
	configuration = PMS.getConfiguration();
	pmsencoder = new PMSEncoder(configuration, matcher);
	customConfigFile = (String)configuration.getCustomProperty(PMSENCODER_CONFIG_FILE_PATH);

	if (customConfigFile) {
	    PMS.minimal("custom config file defined: " + customConfigFile);
	}

	pms = PMS.get();
	extensions = pms.getExtensions();
	pms.registerPlayer(pmsencoder); // XXX this is a private method

	/*
	 * FIXME: don't assume the position is fixed
	 * short term: find and replace *if it exists*
	 * long term: patch PMS to allow plugins to register engines a) separately and b) cleanly
	 * */
	extensions.set(0, new WEB());
    }

    @Override
    String name () {
	"PMSEncoder plugin for PS3 Media Server"
    }

    @Override
    JComponent config () { // no config GUI (though Griffon would make this bearable)
	null
    }

    /*
    @Override
    void nowPlaying(DLNAMediaInfo media, DLNAResource resource) {
	PMS.minimal("PMSEncoder: now playing")
    }

    @Override
    void donePlaying(DLNAMediaInfo media, DLNAResource resource) {
	PMS.minimal("PMSEncoder: done playing")
    }

    @Override
    void shutdown () {
	// nothing to do
    }

    */
}
