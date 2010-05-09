package com.chocolatey.pmsencoder;

import java.util.ArrayList;

import com.chocolatey.pmsencoder.PMSEncoder;

import net.pms.encoders.Player;
import net.pms.PMS;

public class WEB extends net.pms.formats.WEB {
    @Override
    public ArrayList<Class<? extends Player>> getProfiles() {
	ArrayList<Class<? extends Player>> profiles = super.getProfiles();

	if (type == VIDEO) {
	    for (String engine : PMS.getConfiguration().getEnginesAsList(PMS.get().getRegistry())) {
		if (engine.equals(PMSEncoder.ID)) {
		    profiles.add(0, PMSEncoder.class);
		}
	    }
	}

	return profiles;
    }
}
