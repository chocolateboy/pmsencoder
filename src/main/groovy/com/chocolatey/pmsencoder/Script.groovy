@Typed
package com.chocolatey.pmsencoder

public class Script {
    Stage stage
    @Delegate private Matcher matcher

    public Script(Matcher matcher, Stage stage) {
        this.matcher = matcher
        this.stage = stage
    }

    // DSL method
    // XXX more annoying DDWIM magic: Groovy reorders the arguments
    // http://enfranchisedmind.com/blog/posts/groovy-argument-reordering/
    public void profile(Map<String, String> options = [:], String name, Closure closure) throws PMSEncoderException {
        matcher.registerProfile(name, stage, options, closure)
    }
}
