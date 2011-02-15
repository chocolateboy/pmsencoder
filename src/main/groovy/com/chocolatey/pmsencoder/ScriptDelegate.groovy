@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

// i.e. a delegate with access to a Script
public class ScriptDelegate implements LoggerMixin {
    private Script script

    public ScriptDelegate(Script script) {
        this.script = script
    }

    // DSL properties

    // $PMS: getter
    protected final PMS get$PMS() {
        script.$PMS
    }

    // DSL getter: $MENCODER
    protected List<String> get$MENCODER() {
        script.$MENCODER
    }

    // DSL setter: $MENCODER
    protected List<String> set$MENCODER(List args) {
        script.$MENCODER = args*.toString()
    }

    // DSL getter: $MPLAYER
    protected List<String> get$MPLAYER() {
        script.$MPLAYER
    }

    // DSL setter: $MPLAYER
    protected List<String> set$MPLAYER(List args) {
        script.$MPLAYER = args*.toString()
    }

    // DSL getter: $FFMPEG
    protected List<String> get$FFMPEG() {
        script.$FFMPEG
    }

    // DSL setter: $FFMPEG
    protected List<String> set$FFMPEG(List args) {
        script.$FFMPEG = args*.toString()
    }

    // DSL getter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT() {
        script.$YOUTUBE_ACCEPT
    }

    // DSL setter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT(List args) {
        script.$YOUTUBE_ACCEPT = args*.toString()
    }
}
