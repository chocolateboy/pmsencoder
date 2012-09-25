@Typed
package com.chocolatey.pmsencoder

class Pattern {
    @Delegate private ProfileDelegate profileDelegate
    protected static final MatchFailureException STOP_MATCHING = new MatchFailureException()
    // FIXME: sigh: transitive delegation doesn't work (groovy bug)
    @Delegate private final Matcher matcher

    Pattern(ProfileDelegate profileDelegate) {
        this.profileDelegate = profileDelegate
        this.matcher = profileDelegate.matcher
    }

    // DSL setter - overrides the ProfileDelegate method to avoid logging,
    // which is handled later (if the match succeeds) by merging the pattern
    // block's temporary stash
    protected String propertyMissing(String name, String value) {
        command.setVar(name, value)
    }

    protected String propertyMissing(String name) {
        profileDelegate.propertyMissing(name)
    }

    // DSL method
    protected void domain(Object scalarOrList) {
        def uri = command.getVar('uri')
        def u = new URI(uri)
        def domain = u.host
        def matched = false

        if (domain) {
            matched = Util.scalarList(scalarOrList).any({
                return domain.endsWith(it.toString())
            })
        }

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
        def uri = command.getVar('uri')
        def u = new URI(uri)
        def protocol = u.scheme
        def matched = false

        if (protocol) {
            matched = Util.scalarList(scalarOrList).any({
                return protocol == it.toString()
            })
        }

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
    // XXX squashed bug: avoid infinite loop on scrape by explicitly calling it through profileDelegate
    // XXX: we need to declare these signatures separately to work around issues
    // with @Delegate and default parameters
    // note: we don't need to override scrape(Map options) or scrape(Object regex) as they both (via ProfileDelegate)
    // polymorphically call this scrape(Map options, Object regex)
    protected boolean scrape(Map options, Object regex) {
        if (profileDelegate.scrape(options, regex)) {
            return true
        } else {
            throw STOP_MATCHING
        }
    }

    // DSL method: match 'Apple Trailers'
    protected void match(Object object) {
        def matched = true

        // XXX so much for static typing...
        if (object instanceof Closure) {
            matched = matchClosure(object as Closure)
        } else if (object instanceof Map) {
            (object as Map).each { name, value ->
                match(command.getVar(name), value)
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

    // DSL method:
    //    match uri: 'http://www.example.com' // match the URI
    //    match uri: [ 'http://www.foo.com', 'http://www.bar.com' ] // match one of the URIs
    // either (String, String) or (String, List)
    protected void match(Object key, Object value) {
        boolean matched // Groovy++ type inference fail (introduced in 0.4.170)

        if (value instanceof List) {
            matched = (value as List).any({ matchString(key, it) })
        } else {
            matched = matchString(key, value)
        }

        if (!matched) {
            throw STOP_MATCHING
        }
    }

    // DSL method: match { foo > bar }
    private boolean matchClosure(Closure closure) {
        logger.debug('running match block')

        if (closure()) {
            logger.debug('success')
            return true
        } else {
            logger.debug('failure')
            return false
        }
    }

    private boolean matchString(Object name, Object value) {
        if (name == null) {
            logger.error('invalid match: name is not defined')
        } else if (value == null) {
            logger.error('invalid match: value is not defined')
        } else {
            logger.debug("matching $name against $value")

            if (RegexHelper.match(name, value, command.stash)) {
                logger.debug('success')
                return true // abort default failure below
            } else {
                logger.debug("failure")
            }
        }

        return false
    }
}
