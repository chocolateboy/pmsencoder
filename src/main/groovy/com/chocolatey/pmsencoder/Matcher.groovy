@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class Matcher implements LoggerMixin {
    // this is the default Map type, but let's be explicit as we strictly need this type
    protected Map<String, Profile> profiles = new LinkedHashMap<String, Profile>()
    private Script script
    PMS pms

    Matcher(PMS pms) {
        this.pms = pms
        script = new Script(this)
    }

    // for now, this needs to be dynamically-typed
    // @Typed(TypePolicy.DYNAMIC)
    boolean match(Command command, boolean useDefault = true) {
        if (useDefault) {
            script.setDefaultTranscoder(command)
        }

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

        def matched = command.matches.size() > 0

        if (matched) {
            log.trace("command: $command")
        }

        return matched
    }

    void load(String path, String filename = path) {
        load(new File(path), filename)
    }

    void load(URL url, String filename = url.toString()) {
        load(url.openStream(), filename)
    }

    void load(File file, String filename = file.getPath()) {
        load(new FileInputStream(file), filename)
    }

    void load(InputStream stream, String filename) {
        load(new InputStreamReader(stream), filename)
    }

    void load(Reader reader, String filename) {
        def binding = new Binding(script: this.&script)
        def groovy = new GroovyShell(binding)

        groovy.evaluate(reader, filename)
    }

    // DSL method
    void script(Closure closure) { // run at script compile-time
        closure.delegate = script
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }
}
