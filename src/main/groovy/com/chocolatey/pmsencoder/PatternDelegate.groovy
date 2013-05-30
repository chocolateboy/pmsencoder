package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
class PatternDelegate {
    @Delegate private ProfileDelegate profileDelegate
    protected static final MatchFailureException STOP_MATCHING = new MatchFailureException()
    // FIXME: sigh: transitive delegation doesn't work (groovy bug)
    @Delegate private final Matcher matcher

    PatternDelegate(ProfileDelegate profileDelegate) {
        this.profileDelegate = profileDelegate
        this.matcher = profileDelegate.matcher
    }

    // DSL setter - overrides the ProfileDelegate method to avoid logging,
    // which is handled later (if the match succeeds) by merging the pattern
    // block's temporary stash
    protected Object propertyMissing(String name, Object value) {
        getCommand().setVar(name, value)
    }

    // FIXME shouldn't need this; should be provided by the ProfileDelegate @Delegate
    protected Object propertyMissing(String name) {
        profileDelegate.propertyMissing(name)
    }

    // DSL method
    protected void domain(Object maybeList) {
        def uri = getCommand().getVarAsString('uri')
        def u = new URI(uri)
        def domain = u.host
        def matched = false

        if (domain) {
            matched = Util.toStringList(maybeList).any({ String it ->
                return domain.endsWith(it)
            })
        }

        if (!matched) {
            throw STOP_MATCHING
        }
    }

    // DSL method (alias for domain)
    protected void domains(Object maybeList) {
        domain(maybeList)
    }

    // DSL method
    protected void protocol(Object maybeList) {
        def uri = getCommand().getVarAsString('uri')
        def u = new URI(uri)
        def protocol = u.scheme
        def matched = false

        if (protocol) {
            matched = Util.toStringList(maybeList).any({ String it ->
                return protocol == it
            })
        }

        if (!matched) {
            throw STOP_MATCHING
        }
    }

    // DSL method (alias for protocol)
    protected void protocols(Object maybeList) {
        protocol(maybeList)
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
    // polymorphically call this
    protected boolean scrape(Map options, Object regex) {
        if (profileDelegate.scrape(options, regex)) {
            return true
        } else {
            throw STOP_MATCHING
        }
    }

    // DSL method: match
    protected void match(Object object) {
        def matched = true
        def command = getCommand()

        // XXX so much for static typing...
        if (object instanceof Closure) {
            matched = !!matchClosure(object as Closure)
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

            def matchResult = RegexHelper.match(name, value)
            if (matchResult) {
                getCommand().stash.putAll(matchResult.named)
                logger.debug('success')
                return true // abort default failure below
            } else {
                logger.debug("failure")
            }
        }

        return false
    }
}
