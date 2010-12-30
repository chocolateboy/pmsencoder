package com.chocolatey.pmsencoder;

import java.util.ArrayList;

import net.pms.encoders.Player;
import net.pms.PMS;

// XXX this needs to be Java as GMaven doesn't grok template wildcards (? extends Player)
public class WEB extends net.pms.formats.WEB {
    private static final String[] PROTOCOLS = new String[] {
        "br",
        "cue",
        "dvb",
        "dvd",
        "dvdnav",
        "ffmpeg",
        "file",
        "ftp",
        "http",
        "http_proxy",
        "https",
        "icyx",
        "mf",
        "mms",
        "mmsh",
        "mmshttp",
        "mmst",
        "mmsu",
        "mpst",
        "noicyx",
        "pvr",
        "rtp",
        "rtsp",
        "screen",
        "sdp",
        "smb",
        "sop",
        "tivo",
        "tv",
        "udp",
        "unsv",
        "vcd"
    };

    @Override
    public ArrayList<Class<? extends Player>> getProfiles() {
        ArrayList<Class<? extends Player>> profiles = super.getProfiles();

        if (type == VIDEO) {
            for (String engine: PMS.getConfiguration().getEnginesAsList(PMS.get().getRegistry())) {
                if (engine.equals(PMSEncoder.ID)) {
                    profiles.add(0, PMSEncoder.class);
                    break; // ignore duplicates
                }
            }
        }

        return profiles;
    }

    // PMS checks the extension before the protocol, which might cause surprises for e.g.
    // /path/to/foo.cue, so only match on the protocol.
    // XXX this should be fixed in PMS
    @Override
    public boolean match(String filename) {
        boolean match = false;

        if (filename == null) {
            return match;
        }

        filename = filename.toLowerCase();

        for (String id: getId()) {
            match = filename.startsWith(id + "://");

            if (match) {
                matchedId = id;
                return true;
            }
        }

        return match;
    }

    @Override
    public String [] getId() {
        return PROTOCOLS;
    }
}
