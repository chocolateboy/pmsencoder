package com.chocolatey.pmsencoder;

import com.chocolatey.pmsencoder.Engine;

import java.util.ArrayList;

import net.pms.encoders.Player;
import net.pms.PMS;

// XXX this needs to be Java as GMaven doesn't grok template wildcards (? extends Player)
public class WEB extends net.pms.formats.WEB {
    @Override
    public ArrayList<Class<? extends Player>> getProfiles() {
        ArrayList<Class<? extends Player>> profiles = super.getProfiles();

        if (type == VIDEO) {
            for (String engine : PMS.getConfiguration().getEnginesAsList(PMS.get().getRegistry())) {
                if (engine.equals(Engine.ID)) {
                    profiles.add(0, Engine.class);
                }
            }
        }

        return profiles;
    }
}
