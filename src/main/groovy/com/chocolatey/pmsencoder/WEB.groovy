@Typed
package com.chocolatey.pmsencoder

import net.pms.encoders.Player
import net.pms.PMS

public class WEB extends net.pms.formats.WEB {
    private static final String[] PROTOCOLS = [
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
        "pmsencoder",
        "pvr",
        "rtmp",
        "rtmpdump", // pseudo-protocol
        "rtmpe",
        "rtp",
        "rtsp",
        "screen",
        "sdp",
        "smb",
        "sop",
        "synacast",
        "tivo",
        "tv",
        "udp",
        "unsv",
        "vcd"
    ]

    @Override
    public ArrayList<Class<? extends Player>> getProfiles() {
        ArrayList<Class<? extends Player>> profiles = super.getProfiles()

        if (type == VIDEO) {
            for (engine in PMS.getConfiguration().getEnginesAsList(PMS.get().getRegistry())) {
                if (engine == PMSEncoder.ID) {
                    profiles.add(0, PMSEncoder.class)
                    break // ignore duplicates
                }
            }
        }

        return profiles
    }

    // PMS checks the extension before the protocol, which might cause surprises for e.g.
    // /path/to/foo.cue, so only match on the protocol.
    // XXX this should be fixed in PMS
    @Override
    public boolean match(String filename) {
        def match = false

        if (filename == null) {
            return match
        }

        filename = filename.toLowerCase()

        for (id in getId()) {
            match = filename.startsWith(id + "://")

            if (match) {
                matchedId = id
                return true
            }
        }

        return match
    }

    @Override
    public String [] getId() {
        return PROTOCOLS
    }
}
