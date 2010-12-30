@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

// i.e. a delegate with access to a Script
public class ScriptDelegate extends Logger {
    private Script script

    public ScriptDelegate(Script script) {
        this.script = script
    }

    // DSL properties

    // $SCRIPT: getter
    protected final Script get$SCRIPT() {
        script
    }

    // $PMS: getter
    protected final PMS get$PMS() {
        script.$PMS
    }

    // DSL getter: $DEFAULT_TRANSCODER_ARGS
    protected List<String> get$DEFAULT_TRANSCODER_ARGS() {
        script.$DEFAULT_TRANSCODER_ARGS
    }

    // DSL setter: $DEFAULT_TRANSCODER_ARGS
    protected List<String> get$DEFAULT_TRANSCODER_ARGS(List<String> args) {
        script.$DEFAULT_TRANSCODER_ARGS = args
    }

    // DSL getter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT() {
        script.$YOUTUBE_ACCEPT
    }

    // DSL setter: $YOUTUBE_ACCEPT
    protected List<String> get$YOUTUBE_ACCEPT(List<String> args) {
        script.$YOUTUBE_ACCEPT = args
    }
}

