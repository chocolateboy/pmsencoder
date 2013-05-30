package com.chocolatey.pmsencoder

// this holds a reference to the pattern and action blocks, and isn't delegated to
@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
class Profile {
    private final Matcher matcher
    private Closure patternBlock
    private Closure actionBlock
    final Stage stage
    final String name
    final boolean stopOnMatch
    final boolean alwaysRun

    Profile(Matcher matcher, String name, Stage stage, boolean stopOnMatch, boolean alwaysRun) {
        this.matcher = matcher
        this.name = name
        this.stage = stage
        this.stopOnMatch = stopOnMatch
        this.alwaysRun = alwaysRun
    }

    public String toString() {
        """
        {
            name:        $name
            stage:       $stage
            stopOnMatch: $stopOnMatch
            alwaysRun:   $alwaysRun
        }"""
    }

    void extractBlocks(Closure closure) {
        def profileValidationDelegate = new ProfileValidationDelegate(name)
        // wrapper method: runs the closure then validates the result, raising an exception if anything is amiss
        profileValidationDelegate.runProfileBlock(closure)

        // we made it without triggering an exception, so the two fields are sane: save them
        this.patternBlock = profileValidationDelegate.patternBlock // possibly null
        this.actionBlock = profileValidationDelegate.actionBlock   // possibly null
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in PatternDelegate: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    boolean runPatternBlock(PatternDelegate patternDelegate) {
        if (patternBlock == null) {
            // unconditionally match
            logger.trace('no pattern block supplied: matched OK')
        } else {
            // pattern methods short-circuit matching on failure by throwing a MatchFailureException,
            // so we need to wrap this in a try/catch block

            try {
                patternBlock.delegate = patternDelegate
                patternBlock.resolveStrategy = Closure.DELEGATE_FIRST
                patternBlock()
            } catch (MatchFailureException e) {
                logger.trace('pattern block: caught match exception')
                // one of the match methods failed, so the whole block failed
                logger.debug("match $name: failure")
                return false
            }

            // success simply means "no match failure exception was thrown" - this also handles cases where the
            // pattern block is empty
            logger.trace('pattern block: matched OK')
        }

        logger.debug("match $name: success")
        return true
    }

    // pulled out of the match method below so that type-softening is isolated
    // note: keep it here rather than making it a method in ActionDelegate: trying to keep the delegates
    // clean (cf. Ruby's BlankSlate)
    boolean runActionBlock(ProfileDelegate profileDelegate) {
        if (actionBlock != null) {
            def actionDelegate = new ActionDelegate(profileDelegate)
            logger.trace("running action block for: $name")
            actionBlock.delegate = actionDelegate
            actionBlock.resolveStrategy = Closure.DELEGATE_FIRST
            actionBlock()
            logger.trace("finished action block for: $name")
            return true
        } else {
            return false
        }
    }

    boolean match(Command command) {
        if (patternBlock == null && actionBlock == null) {
            return true
        }

        def profileDelegate = new ProfileDelegate(matcher, command)

        if (patternBlock == null) { // unconditionally match
            logger.trace('no pattern block supplied: matched OK')
            runActionBlock(profileDelegate)
            return true
        } else {
            def patternProfileDelegate = new ProfileDelegate(matcher, command)
            def patternDelegate = new PatternDelegate(patternProfileDelegate)

            logger.debug("matching profile: $name")

            // notify the Command that we're processing the pattern block. The Command uses a temporary
            // stash, which a) logs assignments at the quietest level (TRACE) and b) can revert
            // to the previous stash if the pattern match fails (discardStashChanges). If the pattern match
            // succeeds, the changes are merged back into the original stash (with logging) via commitStashChanges()
            //
            // this allows us to discard or mute distracting stash assignment logspam if the match fails,
            // while still allowing us to log the assignments fully if the match succeeds
            command.deferStashChanges()

            if (runPatternBlock(patternDelegate)) { // returns true if all matches in the pattern block succeed, false otherwise
                // first: log and merge any side-effects (i.e. modifications to the stash)
                command.commitStashChanges()
                // now run the actions
                runActionBlock(profileDelegate)
                return true
            } else {
                command.discardStashChanges()
                return false
            }
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
