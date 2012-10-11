@Typed
package com.chocolatey.pmsencoder

import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.Player
import net.pms.io.OutputParams

import org.apache.log4j.Level

/*
 * this object encapsulates the per-request state passed from/to the PMS transcode launcher (PMSEncoder.java).
 */
public class Command implements LoggerMixin {
    private Level defaultStashAssignmentLogLevel = Level.DEBUG

    DLNAMediaInfo media
    DLNAResource dlna
    Level stashAssignmentLogLevel = Level.DEBUG
    List<String> downloader = []
    List<String> hook = []
    List<String> matches = []
    List<String> transcoder = []
    OutputParams params
    Player player
    Stash oldStash
    Stash stash

    public Command() {
        this(new Stash())
    }

    public Command(Stash stash) {
        this.stash = stash
    }

    public Command(List<String> transcoder) {
        this(new Stash(), transcoder)
    }

    public Command(Stash stash, List<String> transcoder) {
        this.stash = stash
        this.transcoder = transcoder
    }

    // convenience constructor: allow the stash to be supplied as a Map<String, String>
    // e.g. new Command([ uri: uri ])
    public Command(Map<String, String> map) {
        // XXX squashed bug: Groovy goes into an infinite loop if this is constructed via: this(new Stash(map))
        this.stash = new Stash(map)
    }

    public java.lang.String toString() {
        def repr = """
        {
            matches:    $matches
            hook:       $hook
            downloader: $downloader
            transcoder: $transcoder
            stash:      $stash
        }""".substring(1).stripIndent(8)

    }

    public void deferStashChanges() {
        assert oldStash == null
        oldStash = stash
        stash = new Stash(stash)
        // avoid clogging up the logfile with pattern-block stash assignments. If the pattern doesn't match,
        // the assignments are irrelevant; and if it does match, the assignments are logged later
        // (when the pattern's temporary stash is merged into the command stash). Rather than suppressing these
        // assignments completely, log them at the lowest (TRACE) level
        stashAssignmentLogLevel = Level.TRACE
    }

    public void discardStashChanges() {
        assert oldStash != null
        stash = oldStash
        oldStash = null
        stashAssignmentLogLevel = defaultStashAssignmentLogLevel
    }

    public void commitStashChanges() {
        assert oldStash != null
        def newStash = stash
        stash = oldStash
        oldStash = null
        // merge (with full logging)
        stashAssignmentLogLevel = defaultStashAssignmentLogLevel
        newStash.each { name, value -> let(name, value) }
    }

    protected boolean hasVar(Object name) {
        stash.containsKey(name.toString())
    }

    protected String getVar(Object name) {
        stash.get(name.toString())
    }

    protected String setVar(String name, String value) {
        let(name.toString(), value.toString())
    }

    // setter implementation with logged stash assignments
    public String let(Object name, Object value) {
        if ((stash.get(name) == null) || (stash.get(name) != value.toString())) {
            if (stashAssignmentLogLevel != null) {
                logger.log(stashAssignmentLogLevel, "setting $name to $value")
            }

            stash.put(name, value)
        }

        return value // for chaining: foo = bar = baz i.e. foo = (bar = baz)
    }
}
