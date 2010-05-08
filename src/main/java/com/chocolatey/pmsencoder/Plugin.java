package com.chocolatey.pmsencoder;

import javax.swing.JComponent;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.external.StartStopListener;
import net.pms.PMS;

class Plugin extends StartStopListener {
    Matcher matcher;
   
    Plugin() {
	matcher = new Matcher(...) // FIXME
    }

    @Override
    void nowPlaying(DLNAMediaInfo media, DLNAResource resource) {
	PMS.minimal("PMSEncoder: now playing") 
    }

    @Override
    void donePlaying(DLNAMediaInfo media, DLNAResource resource) {
	PMS.minimal("PMSEncoder: done playing")
    }

    @Override
    JComponent config () {
	null
    }

    @Override
    String name () {
	"PMSEncoder plugin for PS3 Media Server"
    }

    @Override
    void shutdown () {
	// nothing to do
    }
}
