package com.chocolatey.pmsencoder

import net.pms.encoders.Player
import net.pms.PMS

@groovy.transform.CompileStatic
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
        "pmsencoder", // PMSEncoder pseudo-protocol
        "rtmp",
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
    ]

    @Override
    public String [] getId() {
        return PROTOCOLS
    }

    // WEB should only match on the protocol (and everything else should
    // only match on the extension)
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
}
