@Typed
package com.chocolatey.pmsencoder

import net.pms.io.OutputParams

// i.e. a delegate with access to a Command
// XXX some (most? all?) of these DSL properties could just be exposed/documented as-is i.e.
// log.info(..), http.get(...) &c.
class CommandDelegate extends ScriptDelegate implements LoggerMixin {
    protected Command command
    private final Map<String, String> cache = [:] // only needed/used by this.scrape()
    @Lazy protected HTTPClient http = new HTTPClient()

    public CommandDelegate(Script script, Command command) {
        super(script)
        this.command = command
    }

    // DSL properties

    /*
        XXX Groovy/Groovy++ fail

        if two setters for a property (e.g. $DOWNLOADER) are defined (one for String and another for List<String>)
        Groovy/Groovy++ always uses List<String> and complains at runtime that
        it can't cast a GString into List<String>:

            Cannot cast object '/usr/bin/downloader string http://downloader.string'
            with class 'org.codehaus.groovy.runtime.GStringImpl' to class 'java.util.List'

        workaround: define just one setter and determine the type with instanceof (via stringList)
    */

    // DSL accessor ($ARGS): getter
    protected List<String> get$ARGS() {
        command.args
    }

    // DSL accessor ($ARGS): setter
    // see $DOWNLOADER below for implementation notes
    protected List<String> set$ARGS(Object args) {
        command.args = Util.stringList(args)
    }

    // DSL accessor ($DOWNLOADER): getter
    public List<String> get$DOWNLOADER() {
        command.downloader
    }

    // DSL accessor ($DOWNLOADER): setter
    protected List<String> set$DOWNLOADER(Object downloader) {
        command.downloader = Util.stringList(downloader)
    }

    // DSL accessor ($HOOK): getter
    protected List<String> get$HOOK() {
        command.hook
    }

    // DSL accessor ($HOOK): setter
    protected List<String> set$HOOK(Object hook) {
        command.hook = Util.stringList(hook)
    }

    // $HTTP: getter
    protected HTTPClient get$HTTP() {
        http
    }

    // DSL accessor ($PARAMS): getter
    // $PARAMS: getter
    protected OutputParams get$PARAMS() {
        command.params
    }

    // DSL accessor ($TRANSCODER): getter
    protected List<String> get$TRANSCODER() {
        command.transcoder
    }

    // DSL accessor ($TRANSCODER): setter
    protected List<String> set$TRANSCODER(Object transcoder) {
        command.transcoder = Util.stringList(transcoder)
    }

    // DSL getter
    protected String propertyMissing(String name) throws PMSEncoderException {
        command.stash.get(name)
    }

    // DSL setter
    protected String propertyMissing(String name, Object value) {
        command.let(name, value.toString())
    }

    // DSL method - can be called from a pattern or an action.
    // actions inherit this method, whereas patterns add the
    // short-circuiting behaviour and delegate to this via super.scrape(...)
    protected boolean scrape(Object regex, Map options = [:]) {
        def uri = (options['uri'] == null) ? command.stash.get('$URI') : options['uri'].toString()
        def document = cache[uri]
        def newStash = new Stash()
        def scraped = false

        if (document == null) {
            log.debug("getting $uri")
            assert http != null
            document = cache[uri] = http.get(uri)
        }

        if (document == null) {
            log.error('document not found')
            return scraped
        }

        log.debug("matching content of $uri against $regex")

        if (RegexHelper.match(document, regex.toString(), newStash)) {
            log.debug('success')
            newStash.each { name, value -> command.let(name, value) }
            scraped = true
        } else {
            log.debug('failure')
        }

        return scraped
    }
}
