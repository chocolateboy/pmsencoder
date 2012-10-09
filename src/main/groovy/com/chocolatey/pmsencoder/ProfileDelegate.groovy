@Typed
package com.chocolatey.pmsencoder

import net.pms.dlna.DLNAMediaInfo
import net.pms.dlna.DLNAResource
import net.pms.encoders.Player
import net.pms.io.OutputParams

import org.apache.http.NameValuePair

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

// XXX some (most? all?) of these DSL properties could just be exposed/documented as-is i.e.
// logger.info(..), http.get(...) &c.

/*

    XXX squashed bug: note that delegated methods (i.e. methods exposed via the @Delegate
    annotation) must be *public*:

        All public instance methods present in the type of the annotated field
        and not present in the owner class will be added to owner class
        at compile time.

    http://groovy.codehaus.org/api/groovy/lang/Delegate.html
*/

class ProfileDelegate {
    private final Map<String, String> httpCache = [:] // only needed/used by this.scrape()
    private final Map<String, Document> jsoupCache = [:]
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

        if two or more setters for a property (e.g. downloader) are defined (e.g. one for String and another
        for List<String>) Groovy/Groovy++ only uses one of them, complaining at runtime that
        it can't cast e.g. a String into a List:

            Cannot cast object '/usr/bin/downloader string http://downloader.string'
            with class 'java.lang.String' to class 'java.util.List'

        workaround: define just one setter and determine the type with instanceof (via stringList)
    */

    // DSL accessor (downloader): getter
    public List<String> getDownloader() {
        command.downloader
    }

    // DSL accessor (downloader): setter
    public List<String> setDownloader(Object downloader) {
        command.downloader = Util.stringList(downloader)
    }

    // DSL accessor (transcoder): getter
    public List<String> getTranscoder() {
        command.transcoder
    }

    // DSL accessor (transcoder): setter
    public List<String> setTranscoder(Object transcoder) {
        command.transcoder = Util.stringList(transcoder)
    }

    // DSL accessor (hook): getter
    public List<String> getHook() {
        command.hook
    }

    // DSL accessor (hook): setter
    public List<String> setHook(Object hook) {
        command.hook = Util.stringList(hook)
    }

    // DSL accessor (dlna): getter
    public DLNAResource getDlna() {
        command.dlna
    }

    // DSL accessor (media): getter
    public DLNAMediaInfo getMedia() {
        command.media
    }

    // DSL accessor (params): getter
    public OutputParams getParams() {
        command.params
    }

    // DSL accessor (params): getter
    public Player getPlayer() {
        command.player
    }

    private String getProtocol(Object u) {
        if (u != null) {
            return uri(u.toString()).scheme
        } else {
            return null
        }
    }

    // protocol: getter
    public String getProtocol() {
        return getProtocol(command.getVar('uri'))
    }

    // protocol: setter
    public String setProtocol(Object newProtocol) {
        def u = command.getVar('uri')
        def oldProtocol = getProtocol(u)

        if (oldProtocol) { // not null and not empty
            if (newProtocol == null) {
                newProtocol = ''
            }
            u = newProtocol.toString() + u.substring(oldProtocol.length())
            command.setVar('uri', u)
        }
    }

    // DSL getter
    public Object propertyMissing(String name) {
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
    // delegated to so must be public
    public URI uri() {
        uri(command.getVar('uri'))
    }

    // DSL method
    // delegated to so must be public
    public URI uri(Object u) {
        new URI(u?.toString())
    }

    // DSL method - can be called from a pattern or an action.
    // actions inherit this method, whereas patterns add the
    // short-circuiting behaviour and delegate to this via super.scrape(...)
    // XXX: we need to declare these two signatures explicitly to work around
    // issues with @Delegate and default parameters
    public Function1<Object, Boolean> scrape(Map options) { // curry
        return { Object regex -> scrape(options, regex) }
    }

    // DSL method
    public boolean scrape(Object regex) {
        return scrape([:], regex)
    }

    /*
        1) get the URI pointed to by options['uri'] or command.getVar('uri') (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update the stash with any named captures
    */

    // DSL method
    public boolean scrape(Map options, Object regex) {
        String uri = (options['uri'] == null) ? command.getVar('uri') : options['uri']
        String document = (options['source'] == null) ? httpCache[uri] : options['source']
        boolean decode = options['decode'] == null ? false : options['decode']

        def newStash = new Stash()
        def scraped = false

        if (document == null) {
            logger.debug("getting $uri")
            assert http != null
            document = httpCache[uri] = http.get(uri)
        }

        if (document == null) {
            logger.error('document not found')
            return scraped
        }

        if (decode) {
            logger.debug("URL-decoding content of $uri")
            document = URLDecoder.decode(document)
        }

        logger.debug("matching content of $uri against $regex")

        if (RegexHelper.match(document, regex, newStash)) {
            logger.debug('success')
            newStash.each { name, value -> command.let(name, value) }
            scraped = true
        } else {
            logger.debug('failure')
        }

        return scraped
    }

    // DSL method
    public Function1<Object, Elements> $(Map options) { // curry
        return { Object query -> $(options, query) }
    }

    // DSL method
    public Elements $(Object query) {
        $([:], query)
    }

    // DSL method
    public Elements $(Map options, Object query) {
        def jsoup

        if (options['source']) {
            jsoup = getJsoupForString(options['source'].toString())
        } else if (options['uri']) {
            jsoup = getJsoupForUri(options['uri'].toString())
        } else {
            jsoup = getJsoupForUri(command.getVar('uri'))
        }

        return jsoup.select(query.toString())
    }

    // DSL method
    // spell these out (no default parameters) to work arounbd Groovy bugs
    public Document jsoup() {
        jsoup([:])
    }

    // DSL method
    public Document jsoup(Map options) {
        def jsoup

        if (options['source']) {
            jsoup = getJsoupForString(options['source'].toString())
        } else if (options['uri']) {
            jsoup = getJsoupForUri(options['uri'].toString())
        } else {
            jsoup = getJsoupForUri(command.getVar('uri'))
        }

        return jsoup
    }

    private Document getJsoupForUri(Object obj) {
        def uri = obj.toString()
        def cached = httpCache[uri] ?: (httpCache[uri] = http.get(uri))
        return getJsoupForString(cached)
    }

    private Document getJsoupForString(Object obj) {
        def string = obj.toString()
        return jsoupCache[string] ?: (jsoupCache[string] = Jsoup.parse(string))
    }
}
