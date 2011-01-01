@Typed
package com.chocolatey.pmsencoder

import org.apache.log4j.Level

// this holds a reference to the pattern and action blocks, and isn't delegated to
class Profile implements LoggerMixin {
    private final Script script
    protected Closure patternBlock
    protected Closure actionBlock

    public final String name

    Profile(String name, Script script) {
        this.name = name
        this.script = script
    }

    void extractBlocks(Closure closure) {
        def delegate = new ProfileValidationDelegate(script, name)
        // wrapper method: runs the closure then validates the result, raising an exception if anything is amiss
        delegate.runProfileBlock(closure)

        // we made it without triggering an exception, so the two fields are sane: save them
        this.patternBlock = delegate.patternBlock // possibly null
        this.actionBlock = delegate.actionBlock   // possibly null
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Pattern: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    boolean runPatternBlock(Pattern delegate) {
        if (patternBlock == null) {
            // unconditionally match
            log.trace('no pattern block supplied: matched OK')
        } else {
            // pattern methods short-circuit matching on failure by throwing a MatchFailureException,
            // so we need to wrap this in a try/catch block

            try {
                patternBlock.delegate = delegate
                patternBlock.resolveStrategy = Closure.DELEGATE_FIRST
                patternBlock()
            } catch (MatchFailureException e) {
                log.trace('pattern block: caught match exception')
                // one of the match methods failed, so the whole block failed
                log.debug("match $name: failure")
                return false
            }

            // success simply means "no match failure exception was thrown" - this also handles cases where the
            // pattern block is empty
            log.trace('pattern block: matched OK')
        }

        log.debug("match $name: success")
        return true
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in Action: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    boolean runActionBlock(Action delegate) {
        if (actionBlock != null) {
            actionBlock.delegate = delegate
            actionBlock.resolveStrategy = Closure.DELEGATE_FIRST
            actionBlock()
        } else {
            return true
        }
    }

    boolean match(Command command) {
        def newCommand = new Command(command)

        // avoid clogging up the logfile with pattern-block stash assignments. If the pattern doesn't match,
        // the assigments are irrelevant; and if it does match, the assignments are logged (below)
        // when the pattern's temporary stash is merged into the command stash. Rather than suppressing these
        // assignments completely, log them at the lowest (TRACE) level
        newCommand.setStashAssignmentLogLevel(Level.TRACE)

        // the pattern block has its own command object (which is initially the same as the action block's).
        // if the match succeeds, then the pattern block's stash is merged into the action block's stash.
        // this ensures that a partial match (i.e. a failed match) with side-effects/bindings doesn't contaminate
        // the action, and, more importantly, it defers logging until the whole pattern block has
        // completed successfully
        def pattern = new Pattern(script, newCommand)

        log.debug("matching profile: $name")

        // returns true if all matches in the block succeed, false otherwise
        if (runPatternBlock(pattern)) {
            // we can now merge any side-effects (currently only modifications to the stash).
            // first: merge (with logging)
            newCommand.stash.each { name, value -> command.let(name, value) }
            // now run the actions
            log.trace("running action block for: $name")
            def action = new Action(script, command)
            assert action != null
            runActionBlock(action)
            log.trace("finished action block for: $name")
            return true
        } else {
            return false
        }
    }

    public void assignPatternBlockIfNull(Profile profile) {
        // XXX where is ?= ?
        if (this.patternBlock == null) {
            this.patternBlock = profile.patternBlock
        }
    }

    public void assignActionBlockIfNull(Profile profile) {
        // XXX where is ?= ?
        if (this.actionBlock == null) {
            this.actionBlock = profile.actionBlock
        }
    }
}
