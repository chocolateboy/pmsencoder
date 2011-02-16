@Typed
package com.chocolatey.pmsencoder

import net.pms.io.OutputParams

import org.apache.log4j.Level

/*
 * this object encapsulates the per-request state passed from/to the PMS transcode launcher (PMSEncoder.java).
 */
public class Command implements LoggerMixin, Cloneable {
    Stash stash
    OutputParams params
    Level stashAssignmentLogLevel = Level.DEBUG
    List<String> matches
    List<String> hook
    List<String> downloader
    List<String> transcoder
    List<String> output

    private Command(Stash stash, List<String> transcoder, List<String> matches) {
        this.stash = stash
        this.transcoder = transcoder
        this.matches = matches
    }

    public Command() {
        this(new Stash(), [], [])
    }

    public Command(Stash stash) {
        this(stash, [])
    }

    public Command(List<String> transcoder) {
        this(new Stash(), transcoder)
    }

    public Command(Stash stash, List<String> transcoder) {
        this(stash, transcoder, [])
    }

    // convenience constructor: allow the stash to be supplied as a Map<String, String>
    // e.g. new Command([ uri: uri ])
    public Command(Map<String, String> map) {
        this(new Stash(map), [], [])
    }

    public Command(Command other) {
        this(new Stash(other.stash), new ArrayList<String>(other.transcoder), new ArrayList<String>(other.matches))
    }

    public Command clone() {
        return new Command(this)
    }

    public void setParams(OutputParams params) {
        this.params = params
    }

    public boolean equals(Command other) {
        this.stash == other.stash &&
        this.matches == other.matches &&
        this.params == other.params &&
        this.hook == other.hook &&
        this.downloader == other.downloader &&
        this.transcoder == other.transcoder &&
        this.output == other.output
    }

    public java.lang.String toString() {
        // can't stringify params until this patch has been applied:
        // https://code.google.com/p/ps3mediaserver/issues/detail?id=863
        "{ stash: $stash, matches: $matches, hook: $hook, downloader: $downloader, transcoder: $transcoder }".toString()
    }

    protected boolean hasVar(Object name) {
        stash.containsKey(name)
    }

    protected String getVar(Object name) {
        stash.get(name)
    }

    protected String setVar(Object name, Object value) {
        let(name, value)
    }

    // setter implementation with logged stash assignments
    public String let(Object name, Object value) {
        if ((stash.get(name) == null) || (stash.get(name) != value.toString())) {
            if (stashAssignmentLogLevel != null) {
                log.log(stashAssignmentLogLevel, "setting $name to $value")
            }
            stash.put(name, value)
        }

        return value // for chaining: foo = bar = baz i.e. foo = (bar = baz)
    }
}
