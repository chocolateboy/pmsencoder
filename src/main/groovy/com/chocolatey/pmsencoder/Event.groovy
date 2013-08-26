package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
enum Event {
    FINALIZE('finalizeTranscoderArgs'),
    INCOMPATIBLE('isCompatible'),
    TRANSCODE('launchTranscode');

    private String name

    private Event(String name) {
        this.name = name
    }

    public String toString() { name }
}
