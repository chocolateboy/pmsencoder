@Typed
package com.chocolatey.pmsencoder

import net.pms.encoders.Player
import net.pms.PMS

public class WEB extends net.pms.formats.WEB {
    @Override
    public ArrayList<Class<? extends Player>> getProfiles() {
        ArrayList<Class<? extends Player>> profiles = super.getProfiles()

        if (type == VIDEO) {
            for (engine in  PMS.getConfiguration().getEnginesAsList(PMS.get().getRegistry())) {
                if (engine.equals(Engine.ID)) {
                    profiles.add(0, Engine.class)
                }
            }
        }

        return profiles
    }
}
