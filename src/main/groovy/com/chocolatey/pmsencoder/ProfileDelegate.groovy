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

@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
class ProfileDelegate {
    // FIXME: sigh: transitive delegation doesn't work (groovy bug)
    // so make this public so dependent classes can manually delegate to it
    @Delegate Matcher matcher
    Command command

    // caches (per-request)
    private final Map<String, String> httpCache = [:]
    private final Map<String, Document> jsoupCache = [:]
    private static final int PROCESS_TIMEOUT = 10000
    private static final int TRUNCATE = 16

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

        workaround: define just one setter and determine the type with instanceof (via toStringList)
    */

    // DSL accessor (downloader): getter
    public List<String> getDownloader() {
        command.downloader
    }

    // DSL accessor (downloader): setter
    public List<String> setDownloader(Object downloader) {
        command.downloader = Util.toStringList(downloader, true) // true: split on whitespace if it's a String
    }

    // DSL accessor (transcoder): getter
    public List<String> getTranscoder() {
        command.transcoder
    }

    // DSL accessor (transcoder): setter
    public List<String> setTranscoder(Object transcoder) {
        command.transcoder = Util.toStringList(transcoder, true) // true: split on whitespace if it's a String
    }

    // DSL accessor (hook): getter
    public List<String> getHook() {
        command.hook
    }

    // DSL accessor (hook): setter
    public List<String> setHook(Object hook) {
        command.hook = Util.toStringList(hook, true) // true: split on whitespace if it's a String
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

    public String getProtocol(Object obj) {
        if (obj == null) {
            return null
        } else {
            return uri(obj.toString()).scheme
        }
    }

    // protocol: getter
    public String getProtocol() {
        return getProtocol(command.getVarAsString('uri'))
    }

    // protocol: setter
    public String setProtocol(Object newProtocol) {
        def u = command.getVarAsString('uri')
        String oldProtocol = getProtocol(u)

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
        if (command.hasVar(name)) {
            // inside a pattern or action block
            return command.getVar(name)
        } else {
            // global
            return matcher.getVar(name)
        }
    }

    // DSL setter
    public Object propertyMissing(String name, Object value) {
        command.setVar(name, value)
    }

    // DSL method
    // delegated to so must be public
    public URI uri() {
        uri(command.getVarAsString('uri'))
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
    public Closure<Boolean> scrape(Map options) { // curry
        return { Object regex -> scrape(options, regex) } as Closure<Boolean>
    }

    // DSL method
    public boolean scrape(Object regex) {
        return scrape([:], regex)
    }

    /*
        1) get the URI pointed to by options['uri'] or command.getVarAsString('uri') (if it hasn't already been retrieved)
        2) perform a regex match against the document
        3) update the stash with any named captures
    */

    // DSL method
    public boolean scrape(Map options, Object regex) {
        String uri = (options['uri'] == null) ? command.getVarAsString('uri') : options['uri']
        String document = (options['source'] == null) ? httpCache[uri] : options['source']
        boolean decode = options['decode'] == null ? false : options['decode']

        def scraped = false

        if (document == null) {
            logger.trace("getting $uri")
            document = getHttp().get(uri)
            httpCache[uri] = document
        }

        if (document == null) {
            logger.error('document not found')
            return scraped
        }

        if (decode) {
            logger.trace("URL-decoding content of $uri")
            document = URLDecoder.decode(document)
        }

        if (uri) {
            logger.trace("matching content of $uri against $regex")
        } else {
            logger.trace("matching ${document.take(TRUNCATE).inspect()}... against $regex")
        }

        def matchResult = RegexHelper.match(document, regex)

        if (matchResult) {
            logger.trace('success')
            matchResult.named.each { name, value -> command.setVar(name, value) }
            scraped = true
        } else {
            logger.trace('failure')
        }

        return scraped
    }

    // DSL method
    public Closure<Elements> $(Map options) { // curry
        return { Object query -> $(options, query) } as Closure<Elements>
    }

    // DSL method
    public Elements $(Object query) {
        $([:], query)
    }

    // DSL method
    public Elements $(Map options, Object query) {
        Document jsoup

        if (options['source']) {
            jsoup = getJsoupForString(options['source'].toString())
        } else if (options['uri']) {
            jsoup = getJsoupForUri(options['uri'].toString())
        } else {
            jsoup = getJsoupForUri(command.getVarAsString('uri'))
        }

        return jsoup.select(query.toString())
    }

    // DSL method
    // spell these out (no default parameters) to work around Groovy bugs
    public Document jsoup() {
        jsoup([:])
    }

    // DSL method
    public Document jsoup(Map options) {
        Document jsoup

        if (options['source']) {
            jsoup = getJsoupForString(options['source'].toString())
        } else if (options['uri']) {
            jsoup = getJsoupForUri(options['uri'].toString())
        } else {
            jsoup = getJsoupForUri(command.getVarAsString('uri'))
        }

        return jsoup
    }

    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    private Document getJsoupForUri(Object obj) {
        def uri = obj.toString()
        def cached = httpCache[uri]

        if (cached == null) {
            cached = getHttp().get(uri)
            httpCache[uri] = cached
        }

        return getJsoupForString(cached)
    }

    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    private Document getJsoupForString(Object obj) {
        def string = obj.toString()
        return jsoupCache[string] ?: (jsoupCache[string] = Jsoup.parse(string))
    }

    private Boolean isDownloaderCompatible(Map<String, Boolean> cache, Object maybeList, Object u, List<String> args, String name) {
        URI uri = uri(u)
        String key = String.format('%s://%s', uri.scheme, uri.host)
        Boolean cached = cache.get(key)
        String status = 'cached'

        if (cached == null) {
            status = 'new'
            List<String> command = Util.toStringList(maybeList, true)
            command.addAll(args)
            Process process = command.execute()
            process.consumeProcessOutput()
            process.waitForOrKill(PROCESS_TIMEOUT)
            cached = process.exitValue() == 0
            cache.put(key, cached)
        }

        logger.info("$name compatible: $cached ($status), key: $key, uri: $uri")
        return cached
    }

    // DSL method
    public Boolean isYouTubeDLCompatible(Object maybeList, Object uri) {
        return isDownloaderCompatible(
            getYouTubeDLCache(),
            maybeList,
            uri,
            [ '-g', Util.shellQuote(uri) ],
            'youtube-dl'
        )
    }

    // DSL method
    public Boolean isGetFlashVideosCompatible(Object maybeList, Object uri) {
        return isDownloaderCompatible(
            getGetFlashVideosCache(),
            maybeList,
            uri,
            [ '-i', Util.shellQuote(uri) ],
            'get-flash-videos'
        )
    }
}
