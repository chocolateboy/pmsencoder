@Typed
package com.chocolatey.pmsencoder

import net.pms.encoders.Player
import net.pms.PMS

public class WEB extends net.pms.formats.WEB {
    // unless otherwise indicated, these protocols are only supported by ffmpeg
    // see here for supported ffmpeg protocols: http://ffmpeg.org/pipermail/ffmpeg-cvslog/2011-November/043067.html
    private static final String[] PROTOCOLS = [
        "bluray",
        "concat",
        "ffmpeg",
        "file",
        "gopher",
        "hls",
        "http",
        "https", // undocumented: http://ffmpeg.org/pipermail/ffmpeg-cvslog/2011-November/043067.html
        "mms",
        "mmsh",
        "mmshttp", // probably supported if changed to mmsh
        "mmst",
        "pipe", // ffmpeg - not sure how to use this with PMSEncoder
        "rtmp",
        "rtmpdump", // PMSEncoder pseudo-protocol
        "rtmpe",
        "rtmps",
        "rtmpt",
        "rtmpte",
        "rtp",
        "rtsp",
        "rtsph", // -rtsp_transport http
        "rtspm", // -rtsp_transport udp_multicast
        "rtspt", // -rtsp_transport tcp
        "rtspu", // -rtsp_transport udp
        "sap",
        "tcp",
        "udp",
        "x11grab" // pseudo-protocol for X11 screen capture via ffmpeg
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
