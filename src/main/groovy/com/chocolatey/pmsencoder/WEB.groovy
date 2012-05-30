@Typed
package com.chocolatey.pmsencoder

import net.pms.encoders.Player
import net.pms.PMS

public class WEB extends net.pms.formats.WEB {
    // unless otherwise indicated, these protocols are only supported by MEncoder
    // see here for supported ffmpeg protocols: http://ffmpeg.org/pipermail/ffmpeg-cvslog/2011-November/043067.html
    private static final String[] PROTOCOLS = [
        "bluray", // ffmpeg
        "br",
        "cdda",
        "cddb",
        "concat", // ffmpeg
        "cue",
        "dvb",
        "dvd",
        "dvdnav",
        "ffmpeg",
        "file", // ffmpeg and mencoder
        "ftp",
        "gopher", // ffmpeg (for some reason)
        "hls", // ffmpeg
        "http", // ffmpeg and mencoder
        "http_proxy",
        "https", // ffmpeg (undocumented: http://ffmpeg.org/pipermail/ffmpeg-cvslog/2011-November/043067.html) and mencoder
        "icyx",
        "mf",
        "mms",
        "mmsh", // ffmpeg and mencoder (broken)
        "mmshttp", // probably ffmpeg if changed to mmsh; probably broken in mencoder
        "mmst", // ffmpeg and mencoder (probably broken)
        "mmsu",
        "mpst",
        "navix", // PMSEncoder pseudo-protocol
        "noicyx",
        "pipe", // ffmpeg - not sure how to use this with PMSEncoder
        "pvr",
        "radio",
        "rtmpdump", // PMSEncoder pseudo-protocol
        "rtmpe", // ffmpeg and mencoder
        "rtmp", // ffmpeg and mencoder
        "rtmps", // ffmpeg
        "rtmpte", // ffmpeg
        "rtmpt", // ffmpeg
        "rtp", // ffmpeg and mencoder
        "rtsp",
        "sap", // ffmpeg
        "screen",
        "sdp",
        "smb",
        "sop",
        "synacast",
        "tcp", // ffmpeg
        "tivo",
        "tv",
        "udp", // ffmpeg and mencoder
        "unsv",
        "vcd",
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
