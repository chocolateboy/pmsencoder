@Typed
package com.chocolatey.pmsencoder

import net.pms.io.OutputParams

// i.e. a delegate with access to a Command
// XXX some (most? all?) of these DSL properties could just be exposed/documented as-is i.e.
// log.info(..), http.get(...) &c.
// XXX: also, several probably shouldn't be exposed: $MATCHES, $STASH, $COMMAND &c.
public class CommandDelegate extends ScriptDelegate {
    private Command command
    private final Map<String, String> cache = [:] // only needed/used by scrape()
    @Lazy protected HTTPClient http = new HTTPClient()

    public CommandDelegate(Script script, Command command) {
        super(script)
        this.command = command
    }

    // DSL properties

    // DSL accessor ($ARGS): getter
    protected List<String> get$ARGS() {
        command.args
    }

    // handle values that can be a String or a List.
    // split the former along whitespace and return the latter as-is
    @Typed(TypePolicy.DYNAMIC) // try to handle GStrings
    private List<String> stringList(Object stringOrList) {
        def list = ((stringOrList instanceof List) ? stringOrList : stringOrList.toString().tokenize()) as List
        return list.collect { it.toString() }
    }

    // DSL accessor ($ARGS): setter
    // see $DOWNLOADER below for implementation notes
    protected List<String> set$ARGS(Object args) {
        command.args = stringList(args)
    }

    // $COMMAND: getter
    protected Command get$COMMAND() {
        command
    }

    // DSL accessor ($DOWNLOADER): getter
    protected List<String> get$DOWNLOADER() {
        command.downloader
    }

    /*
        XXX Groovy/Groovy++ fail

        if two setters for $DOWNLOADER are defined (one for String and another for List<String>)
        Groovy/Groovy++ always uses List<String> and complains at runtime that
        it can't cast a GString into List<String>:

            Cannot cast object '/usr/bin/downloader string http://www.downloader-string.com'
            with class 'org.codehaus.groovy.runtime.GStringImpl' to class 'java.util.List'

        workaround: define just one setter and determine the type with instanceof
    */

    // DSL accessor ($DOWNLOADER): setter
    protected List<String> set$DOWNLOADER(Object downloader) {
        command.downloader = stringList(downloader)
    }

    // DSL accessor ($HOOK): getter
    protected List<String> get$HOOK() {
        command.hook
    }

    // DSL accessor ($HOOK): setter
    // see $DOWNLOADER above for implementation notes
    protected List<String> set$HOOK(Object hook) {
        command.hook = stringList(hook)
    }

    // $HTTP: getter
    protected HTTPClient get$HTTP() {
        http
    }

    // $LOGGER: getter
    protected org.apache.log4j.Logger get$LOGGER() {
        log
    }

    // DSL accessor ($MATCHES): getter
    protected List<String> get$MATCHES() {
        command.matches
    }

    // $PARAMS: getter
    public OutputParams get$PARAMS() {
        command.params
    }

    // $STASH: getter
    protected final Stash get$STASH() {
        command.stash
    }

    // DSL accessor ($TRANSCODER): getter
    protected List<String> get$TRANSCODER() {
        command.transcoder
    }

    // DSL accessor ($TRANSCODER): setter
    // see $DOWNLOADER above for implementation notes
    protected List<String> set$TRANSCODER(Object transcoder) {
        command.transcoder = stringList(transcoder)
    }

    // DSL getter
    protected String propertyMissing(String name) throws PMSEncoderException {
        command.stash[name]
    }

    // DSL setter
    protected String propertyMissing(String name, Object value) {
        assert value != null
        command.let(name, value.toString())
    }

    // DSL method - can be called from a pattern or an action.
    // actions inherit this method, whereas patterns add the
    // short-circuiting behaviour and delegate to this via super.scrape(...)
    @Typed(TypePolicy.MIXED) // XXX try to handle GStrings
    boolean scrape(String regex, Map<String, String> options = [:]) {
        def uri = options['uri'] ?: $STASH['$URI']
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

        if (RegexHelper.match(document, regex, newStash)) {
            log.debug('success')
            newStash.each { name, value -> command.let(name, value) }
            scraped = true
        } else {
            log.debug('failure')
        }

        return scraped
    }
}

