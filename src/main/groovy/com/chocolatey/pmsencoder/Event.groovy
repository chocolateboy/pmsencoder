package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
enum Event {
    TRANSCODE("launchTranscode"),
    FINALIZE("finalizeTranscoderArgs");

    private String name

    private Event(String name) {
        this.name = name
    }

    public String toString() { name }
}
