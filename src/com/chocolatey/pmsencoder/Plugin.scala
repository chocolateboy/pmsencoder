package com.chocolatey.pmsencoder

import javax.swing.JComponent

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.{DLNAMediaInfo,DLNAResource}
import net.pms.external.StartStopListener;
import net.pms.PMS;

object Plugin extends StartStopListener {
    private val configuration = PMS.getConfiguration();
    private val pmsencoder = new PMSEncoder(configuration);
    private val config_file_path = configuration.getCustomProperty(PMSENCODER_CONFIG_FILE_PATH).asInstanceOf[String];
    private val PMSENCODER_CONFIG_FILE_PATH = "pmsencoder.config_file";
    private val pms = PMS.get();
    private val extensions = pms.getExtensions();

    if (config_file_path != null) {
        PMS.minimal("Got config file path: " + config_file_path);
    }
            
    pms.registerPlayer(pmsencoder);

    /*
     * FIXME: don't assume the position is fixed
     * short term: find and replace *if it exists*
     * long term: patch PMS to allow plugins to register engines a) separately and b) cleanly  
     * */
    extensions.set(0, new WEB());

    PMS.minimal("YAY! Initialized PMSEncoder!");

    def nowPlaying(media: DLNAMediaInfo, resource: DLNAResource) = {
	PMS.minimal("PMSEncoder: now playing"); 
    }

    def donePlaying(media: DLNAMediaInfo, resources: DLNAResource) = {
	PMS.minimal("PMSEncoder: done playing"); 
    }

    def config : JComponent = null

    def name = "PMSEncoder plugin for PS3 Media Server"

    def shutdown = {
	// nothing to do
    }
}
