package com.chocolatey.pmsencoder;

import scala.collection.JavaConversions._
import scala.util.control.Breaks._

import java.util.ArrayList;

import com.chocolatey.pmsencoder._;

import net.pms.encoders.Player;
import net.pms.formats.Format;
import net.pms.PMS;

class WEB extends net.pms.formats.WEB {
    override def getProfiles(): ArrayList[Class[_ <: Player]] = {
	var profiles = super.getProfiles();

        if (getType() == Format.VIDEO) { // we could use `type`, but this is cleaner
            breakable {
                for (engine <- PMS.getConfiguration.getEnginesAsList(PMS.get.getRegistry)) {
                    if (engine == PMSEncoder.ID) {
                        profiles.add(0, classOf[PMSEncoder]);
                        break; // added in Scala 2.8
                    }
                }
            }
        }

	return profiles;
    }
}
