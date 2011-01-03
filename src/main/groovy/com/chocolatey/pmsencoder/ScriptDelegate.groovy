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

    // DSL getter: $DEFAULT_MENCODER_ARGS
    protected List<String> get$DEFAULT_MENCODER_ARGS() {
        script.$DEFAULT_MENCODER_ARGS
    }

    // DSL setter: $DEFAULT_MENCODER_ARGS
    protected List<String> set$DEFAULT_MENCODER_ARGS(List args) {
        script.$DEFAULT_MENCODER_ARGS = args*.toString()
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
