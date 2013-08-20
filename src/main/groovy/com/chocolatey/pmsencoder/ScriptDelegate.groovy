package com.chocolatey.pmsencoder

@groovy.transform.CompileStatic
public class ScriptDelegate {
    @Delegate private Matcher matcher
    final Stage stage // read-only

    public ScriptDelegate(Matcher matcher, Stage stage) {
        this.matcher = matcher
        this.stage = stage
    }

    // DSL method
    // XXX more annoying DDWIM magic: Groovy reorders the arguments
    // http://enfranchisedmind.com/blog/posts/groovy-argument-reordering/
    public void profile(Map<String, Object> options = [:], String name, Closure closure) throws PMSEncoderException {
        matcher.registerProfile(name, stage, options, closure)
    }
}
