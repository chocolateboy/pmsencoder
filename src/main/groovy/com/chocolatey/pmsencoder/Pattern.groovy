@Typed
package com.chocolatey.pmsencoder

class Pattern extends CommandDelegate {
    private static final MatchFailureException STOP_MATCHING = new MatchFailureException()

    Pattern(Script script, Command command) {
        super(script, command)
    }

    // DSL setter - overrides the CommandDelegate method to avoid logging,
    // which is handled later (if the match succeeds) by merging the pattern
    // block's temporary stash
    protected String propertyMissing(String name, Object value) {
        $STASH[name] = value
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domain(String name) {
        if (!matchString($STASH['$URI'], domainToRegex(name.toString()))) {
            throw STOP_MATCHING
        }
    }

    // DSL method (alias for domain)
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domains(String name) {
        domain(name)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domain(List<String> domains) {
        if (!(domains.any { name -> matchString($STASH['$URI'], domainToRegex(name.toString())) })) {
            throw STOP_MATCHING
        }
    }

    // DSL method (alias for domain)
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void domains(List<String> domains) {
        domain(domains)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void protocol(String scheme) {
        if (!matchString($STASH['$URI'], "^${scheme}://")) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void protocol(List<String> schemes) {
        if (!(schemes.any { scheme -> matchString($STASH['$URI'], "^${scheme}://") })) {
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
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    boolean scrape(String regex, Map<String, String> options = [:]) {
        if (super.scrape(regex, options)) {
            return true
        } else {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(Map<String, Object> map) {
        map.each { name, value ->
            def list = (value instanceof List) ? value as List : [ value ]
            match($STASH[name.toString()], list)
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name, List values) {
        if (!(values.any { value -> matchString(name.toString(), value.toString()) })) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name, Object value) {
        def list = (value instanceof List) ? value as List : [ value ]
        match(name.toString(), list)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(String name) {
        if (!$MATCHES.contains(name.toString())) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(List<String> names) {
        if (!$MATCHES.containsAll(names)) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void match(Closure closure) {
        log.debug('running match block')

        if (closure()) {
            log.debug('success')
        } else {
            log.debug('failure')
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void reject(Map<String, Object> map) {
        map.each { name, value ->
            def list = (value instanceof List) ? value as List : [ value ]
            reject($STASH[name.toString()], list)
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void reject(String name, List values) {
        if (values.any { value -> matchString(name.toString(), value.toString()) }) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void reject(String name, Object value) {
        def list = (value instanceof List) ? value as List : [ value ]
        reject(name.toString(), list)
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void reject(String name) {
        if ($MATCHES.contains(name.toString())) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void reject(List<String> names) {
        if ($MATCHES.containsAll(names)) {
            throw STOP_MATCHING
        }
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    void reject(Closure closure) {
        log.debug('running reject block')

        if (closure()) {
            log.debug('failure')
            throw STOP_MATCHING
        } else {
            log.debug('success')
        }
    }

    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    private boolean matchString(String name, String value) {
        if (name == null) {
            log.error('invalid match: name is not defined')
        } else if (value == null) {
            log.error('invalid match: value is not defined')
        } else {
            log.debug("matching $name against $value")

            if (RegexHelper.match(name.toString(), value.toString(), $STASH)) {
                log.debug('success')
                return true // abort default failure below
            } else {
                log.debug("failure")
            }
        }

        return false
    }

    // DSL method
    @Typed(TypePolicy.DYNAMIC) // XXX try to handle GStrings
    protected String domainToRegex(String domain) {
        def escaped = domain.replaceAll('\\.', '\\\\.')
        return "^https?://(\\w+\\.)*${escaped}(/|\$)".toString()
    }
}
