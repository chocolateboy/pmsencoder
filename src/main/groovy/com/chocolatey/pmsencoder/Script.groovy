@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class Script implements LoggerMixin {
    // this is the default Map type, but let's be explicit for documentation purposes
    private Map<String, Profile> profiles = new LinkedHashMap<String, Profile>()
    // DSL fields (mutable)
    protected List<String> $MENCODER = []
    protected List<String> $MPLAYER = []
    protected List<String> $FFMPEG = []
    public List<Integer> $YOUTUBE_ACCEPT = []
    public PMS $PMS
    private Map<String, Object> stash = new HashMap<String, Object>()

    public Script(PMS pms) {
        $PMS = pms
    }

    boolean match(Command command) {
        def uri = command.stash.get('$URI')
        log.debug("matching URI: $uri")

        profiles.values().each { profile ->
            if (profile.match(command)) {
                // XXX make sure we take the name from the profile itself
                // rather than the Map key - the latter may have been usurped
                // by a profile with a different name
                command.matches << profile.name
            }
        }

        return command.matches.size() > 0
    }

    // DSL method
    void script(Closure closure) { // run at script compile-time
        closure.delegate = this
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    Object propertyMissing(String name) {
        log.debug("retrieving global variable: $name")
        return stash[name]
    }

    Object propertyMissing(String name, Object value) {
        log.debug("setting global variable: $name = $value")
        return stash[name] = value
    }

    // DSL method
    // a Profile consists of a name, a pattern block and an action block - all
    // determined when the script is loaded/compiled
    // XXX more annoying DDWIM magic: Groovy reorders the arguments
    // http://enfranchisedmind.com/blog/posts/groovy-argument-reordering/
    protected void profile (Map<String, String> options = [:], String name, Closure closure) throws PMSEncoderException {
        def extendz = options['extends']
        def replaces = options['replaces']
        def target

        if (replaces != null) {
            target = replaces
            log.info("replacing profile $replaces with: $name")
        } else {
            target = name
            if (profiles[name] != null) {
                log.info("replacing profile: $name")
            } else {
                log.info("registering profile: $name")
            }
        }

        def profile = new Profile(name, this)

        try {
            // run the profile block at compile-time to extract its pattern and action blocks,
            // but invoke them at runtime
            profile.extractBlocks(closure)

            if (extendz != null) {
                if (profiles[extendz] == null) {
                    log.error("attempt to extend a nonexistent profile: $extendz")
                } else {
                    def base = profiles[extendz]
                    profile.assignPatternBlockIfNull(base)
                    profile.assignActionBlockIfNull(base)
                }
            }

            // this is why name is defined both as the key of the map and in the profile
            // itself. the key allows replacement
            profiles[target] = profile
        } catch (Throwable e) {
            log.error("invalid profile ($name): " + e.getMessage())
        }
    }
}

