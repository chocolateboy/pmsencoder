@Typed
package com.chocolatey.pmsencoder

import net.pms.io.OutputParams
import geb.*
import org.openqa.selenium.WebDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.NameValuePair

// XXX some (most? all?) of these DSL properties could just be exposed/documented as-is i.e.
// log.info(..), http.get(...) &c.

/*

    XXX squashed bug: note that delegated methods (i.e. methods exposed via the @Delegate
    annotation) must be *public*:

        All public instance methods present in the type of the annotated field
        and not present in the owner class will be added to owner class
        at compile time.

    http://groovy.codehaus.org/api/groovy/lang/Delegate.html
*/

class ProfileDelegate {
    private final Map<String, String> cache = [:] // only needed/used by this.scrape()
    @Lazy private HTTPClient http = new HTTPClient()
    @Lazy private WebDriver driver = new HtmlUnitDriver()
    // FIXME: sigh: transitive delegation doesn't work (groovy bug)
    // so make this public so dependent classes can manually delegate to it
    @Delegate Matcher matcher
    Command command

    public ProfileDelegate(Matcher matcher, Command command) {
        this.matcher = matcher
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

    // DSL accessor ($DOWNLOADER): getter
    public List<String> get$DOWNLOADER() {
        command.downloader
    }

    // DSL accessor ($DOWNLOADER): setter
    public List<String> set$DOWNLOADER(Object downloader) {
        command.downloader = Util.stringList(downloader)
    }

    // DSL accessor ($TRANSCODER): getter
    public List<String> get$TRANSCODER() {
        command.transcoder
    }

    // DSL accessor ($TRANSCODER): setter
    public List<String> set$TRANSCODER(Object transcoder) {
        command.transcoder = Util.stringList(transcoder)
    }

    // DSL accessor ($HOOK): getter
    public List<String> get$HOOK() {
        command.hook
    }

    // DSL accessor ($HOOK): setter
    public List<String> set$HOOK(Object hook) {
        command.hook = Util.stringList(hook)
    }

    // DSL accessor ($OUTPUT): getter
    public List<String> get$OUTPUT() {
        command.output
    }

    // DSL accessor ($OUTPUT): setter
    public List<String> set$OUTPUT(Object args) {
        command.output = Util.stringList(args)
    }

    // $HTTP: getter
    public HTTPClient get$HTTP() {
        http
    }

    // FIXME: use the URI class
    private String getProtocol(String uri) {
        if (uri != null) {
            return RegexHelper.match(uri, '^(\\w+)://')[1]
        } else {
            return null
        }
    }

    // $PROTOCOL: getter
    public String get$PROTOCOL() {
        return getProtocol(command.getVar('$URI'))
    }

    // DSL accessor ($PARAMS): getter
    // $PARAMS: getter
    public OutputParams get$PARAMS() {
        command.params
    }

    // DSL getter
    public String propertyMissing(String name) {
        if (matcher.hasVar(name)) {
            return matcher.getVar(name)
        } else {
            return command.getVar(name)
        }
    }

    // DSL setter
    public String propertyMissing(String name, Object value) {
        command.let(name, value?.toString())
    }

    // DSL method
    public Object browse(Map options = [:], Closure closure) {
        String uri = (options['uri'] == null) ? command.getVar('$URI') : options['uri'].toString()
        driver.get(uri)
        Browser.drive(driver, closure)
    }

    // DSL method - can be called from a pattern or an action.
    // actions inherit this method, whereas patterns add the
    // short-circuiting behaviour and delegate to this via super.scrape(...)
    public boolean scrape(Object regex, Map options = [:]) {
        String uri = (options['uri'] == null) ? command.getVar('$URI') : options['uri'].toString()
        String document = (options['source'] == null) ? cache[uri] : options['source'].toString()
        boolean decode = options['decode'] == null ? false : options['decode']

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
