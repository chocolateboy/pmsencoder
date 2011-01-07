@Typed
package com.chocolatey.pmsencoder

import net.pms.io.OutputParams
import geb.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver

// i.e. a delegate with access to a Command
// XXX some (most? all?) of these DSL properties could just be exposed/documented as-is i.e.
// log.info(..), http.get(...) &c.
class CommandDelegate extends ScriptDelegate implements LoggerMixin {
    protected Command command
    private final Map<String, String> cache = [:] // only needed/used by this.scrape()
    @Lazy protected HTTPClient http = new HTTPClient()
    @Lazy private WebDriver driver = new HtmlUnitDriver()

    public CommandDelegate(Script script, Command command) {
        super(script)
        this.command = command
    }

    // DSL properties

    /*
        XXX Groovy fail: http://jira.codehaus.org/browse/GROOVY-2500

        if two or more setters for a property (e.g. $DOWNLOADER) are defined (e.g. one for String and another
        for List<String>) Groovy/Groovy++ only uses one of them, complaining at runtime that
        it can't cast e.g. a String into a List:

            Cannot cast object '/usr/bin/downloader string http://downloader.string'
            with class 'java.lang.String' to class 'java.util.List'

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
        if (value == null) {
            log.error("attempt to assign a null value for: $name")
            return null
        } else {
            command.let(name, value.toString())
        }
    }

    // DSL method
    protected Object browse(Map options = [:], Closure closure) {
        String uri = (options['uri'] == null) ? command.stash.get('$URI') : options['uri'].toString()
        driver.get(uri)
        Browser.drive(driver, closure)
    }

    // DSL method - can be called from a pattern or an action.
    // actions inherit this method, whereas patterns add the
    // short-circuiting behaviour and delegate to this via super.scrape(...)
    protected boolean scrape(Object regex, Map options = [:]) {
        String uri = (options['uri'] == null) ? command.stash.get('$URI') : options['uri'].toString()
        String document = (options['source'] == null) ? cache[uri] : options['source'].toString()
        boolean decode = options['decode'] ?: false

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

        if (decode) {
            log.debug("URL-decoding content of $uri")
            document = URLDecoder.decode(document)
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
