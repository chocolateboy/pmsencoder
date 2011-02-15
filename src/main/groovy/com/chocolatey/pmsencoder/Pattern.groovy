@Typed
package com.chocolatey.pmsencoder

class Pattern {
    @Delegate private ProfileDelegate profileDelegate
    protected static final MatchFailureException STOP_MATCHING = new MatchFailureException()
    // FIXME: sigh: transitive delegation doesn't work (groovy bug)
    @Delegate private final Script script

    Pattern(ProfileDelegate profileDelegate) {
        this.profileDelegate = profileDelegate
        this.script = profileDelegate.script
    }

    // DSL setter - overrides the ProfileDelegate method to avoid logging,
    // which is handled later (if the match succeeds) by merging the pattern
    // block's temporary stash
    protected String propertyMissing(String name, Object value) {
        command.stash.put(name, value)
    }

    protected String propertyMissing(String name) {
        profileDelegate.propertyMissing(name)
    }

    // DSL method
    protected void domain(Object scalarOrList) {
        def uri = command.stash.get('$URI')
        def matched = Util.scalarList(scalarOrList).any({
            return matchString(uri, domainToRegex(it))
        })

        if (!matched) {
            throw STOP_MATCHING
        }
    }

    // DLS method (alias for domain)
    protected void domains(Object scalarOrList) {
        domain(scalarOrList)
    }

    // DSL method
    protected void protocol(Object scalarOrList) {
        def matched = Util.scalarList(scalarOrList).any({
            return command.stash.get('$URI').startsWith("${it}://".toString())
        })

        if (!matched) {
            throw STOP_MATCHING
        }
    }

    /*
        We don't have to worry about stash-assignment side-effects, as they're
        only committed if the whole pattern block succeeds. This is handled
        up the call stack (in Profile.match)
    */

    // DSL method
    @Override // for documentation; Groovy doesn't require it
    protected boolean scrape(Object regex, Map options = [:]) {
        if (scrape(regex, options)) {
            return true
        } else {
            throw STOP_MATCHING
        }
    }

    // DSL method
    // XXX so much for static typing...
    protected void match(Object object) {
        def matched = true

        if (object instanceof Closure) {
            matched = matchClosure(object as Closure)
        } else if (object instanceof Map) {
            (object as Map).each { name, value ->
                match(command.stash.get(name), value)
            }
        } else if (object instanceof List) {
            def matches = (object as List)*.toString()
            matched = command.matches.containsAll(matches)
        } else {
            matched = command.matches.contains(object.toString())
        }

        if (!matched) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    // either (String , String) or (String, List)
    protected void match(Object key, Object value) {
        def matched

        if (value instanceof List) {
            matched = (value as List).any({ matchString(key, it) })
        } else {
            matched = matchString(key, value)
        }

        if (!matched) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    private boolean matchClosure(Closure closure) {
        log.debug('running match block')

        if (closure()) {
            log.debug('success')
            return true
        } else {
            log.debug('failure')
            return false
        }
    }

    private boolean matchString(Object name, Object value) {
        if (name == null) {
            log.error('invalid match: name is not defined')
        } else if (value == null) {
            log.error('invalid match: value is not defined')
        } else {
            log.debug("matching $name against $value")

            if (RegexHelper.match(name, value, command.stash)) {
                log.debug('success')
                return true // abort default failure below
            } else {
                log.debug("failure")
            }
        }

        return false
    }

    // DSL method
    protected String domainToRegex(Object domain) {
        def escaped = domain.toString().replaceAll('\\.', '\\\\.')
        return "^https?://(\\w+\\.)*${escaped}(/|\$)".toString()
    }
}
