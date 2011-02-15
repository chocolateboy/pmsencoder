@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

// i.e. a delegate with access to a Matcher
public class Script implements LoggerMixin {
    private Matcher matcher
    private PMS pms
    private List<String> mencoder = []
    private List<String> mplayer = []
    private List<String> ffmpeg = []
    private List<Integer> youtubeAccept = []
    Map<String, Object> stash = new HashMap<String, Object>()

    public Script(Matcher matcher) {
        this.matcher = matcher
    }

    Object propertyMissing(String name) {
        log.debug("retrieving global variable: $name")
        return stash[name]
    }

    Object propertyMissing(String name, Object value) {
        log.debug("setting global variable: $name = $value")
        return stash[name] = value
    }

    protected void setDefaultTranscoder(Command command) {
        command.transcoder = ffmpeg*.toString()
    }

    // DSL properties

    // $PMS: getter
    public final PMS get$PMS() {
        matcher.pms
    }

    // DSL getter: $MENCODER
    public List<String> get$MENCODER() {
        mencoder
    }

    // DSL setter: $MENCODER
    public List<String> set$MENCODER(List args) {
        mencoder = args*.toString()
    }

    // DSL getter: $MPLAYER
    public List<String> get$MPLAYER() {
        mplayer
    }

    // DSL setter: $MPLAYER
    public List<String> set$MPLAYER(List args) {
        mplayer = args*.toString()
    }

    // DSL getter: $FFMPEG
    public List<String> get$FFMPEG() {
        ffmpeg
    }

    // DSL setter: $FFMPEG
    public List<String> set$FFMPEG(List args) {
        ffmpeg = args*.toString()
    }

    // DSL getter: $YOUTUBE_ACCEPT
    public List<Integer> get$YOUTUBE_ACCEPT() {
        youtubeAccept
    }

    // DSL setter: $YOUTUBE_ACCEPT
    public List<Integer> set$YOUTUBE_ACCEPT(List<Integer> args) {
        youtubeAccept = args
    }

    // DSL method
    // a Profile consists of a name, a pattern block and an action block - all
    // determined when the script is loaded/compiled
    // XXX more annoying DDWIM magic: Groovy reorders the arguments
    // http://enfranchisedmind.com/blog/posts/groovy-argument-reordering/
    public void profile(Map<String, String> options = [:], String name, Closure closure) throws PMSEncoderException {
        def extendz = options['extends']
        def replaces = options['replaces']
        def target

        if (replaces != null) {
            target = replaces
            log.info("replacing profile $replaces with: $name")
        } else {
            target = name
            if (matcher.profiles[name] != null) {
                log.info("replacing profile: $name")
            } else {
                log.info("registering profile: $name")
            }
        }

        def profile = new Profile(this, name)

        try {
            // run the profile block at compile-time to extract its pattern and action blocks,
            // but invoke them at runtime
            profile.extractBlocks(closure)

            if (extendz != null) {
                if (matcher.profiles[extendz] == null) {
                    log.error("attempt to extend a nonexistent profile: $extendz")
                } else {
                    def base = matcher.profiles[extendz]
                    profile.assignPatternBlockIfNull(base)
                    profile.assignActionBlockIfNull(base)
                }
            }

            // this is why name is defined both as the key of the map and in the profile
            // itself. the key allows replacement
            matcher.profiles[target] = profile
        } catch (Throwable e) {
            log.error("invalid profile ($name): " + e.getMessage())
        }
    }
}
