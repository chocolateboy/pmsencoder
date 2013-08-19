package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
enum Event {
    TRANSCODE {
        public String toString() {
            "launchTranscode"
        }
    },
    FINALIZE {
        public String toString() {
            "finalizeTranscoderArgs"
        }
    }
}
