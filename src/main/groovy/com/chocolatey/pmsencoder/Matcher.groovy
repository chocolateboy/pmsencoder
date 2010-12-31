@Typed
package com.chocolatey.pmsencoder

import net.pms.PMS

class Matcher extends Logger {
    // FIXME: this is only public for a (single) test
    // 1) Script has the same scope as Matcher so they could be merged,
    // but we don't want to expose load()
    // 2) the script "globals" (e.g. $DEFAULT_MENCODER_ARGS) could be moved here
    public final Script script

    Matcher(PMS pms) {
        this.script = new Script(pms)
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
        def binding = new Binding(script: script.&script)
        def groovy = new GroovyShell(binding)

        groovy.evaluate(reader, filename)
    }

    @Typed(TypePolicy.DYNAMIC) // XXX needed to handle GStrings
    boolean match(Command command, boolean useDefault = true) {
        if (useDefault) {
            // watch out: there's a GString about
            script.$DEFAULT_MENCODER_ARGS.each { command.args << it.toString() }
        }

        def matched = script.match(command) // we could use the @Delegate annotation, but this is cleaner/clearer

        if (matched) {
            log.trace("command: $command")
        }

        return matched
    }
}

