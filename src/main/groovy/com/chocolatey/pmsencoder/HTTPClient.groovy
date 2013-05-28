package com.chocolatey.pmsencoder

import groovy.json.JsonSlurper
import groovy.util.slurpersupport.GPathResult

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ParserRegistry

import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.HttpHost
import org.apache.http.NameValuePair
import org.apache.http.protocol.ExecutionContext
import org.apache.http.protocol.HttpContext

import groovyx.net.http.ContentType
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.HEAD

import java.nio.charset.Charset

// return types taken from:
// ParserRegistry: http://tinyurl.com/395cjkb

// XXX the URLENC type can probably be used to simplify YouTube fmt_url_map handling

@groovy.transform.CompileStatic
@groovy.util.logging.Log4j(value="logger")
class HTTPClient {
    final static private DEFAULT_CHARSET = 'UTF-8'
    private JsonSlurper jsonSlurper = new JsonSlurper()

    // these methods are called from scripts so need to be forgiving (Object) in what
    // they receive

    public String get(Object uri) { // i.e. get text
        getType(uri?.toString(), ContentType.TEXT)?.toString()
    }

    public GPathResult getXML(Object uri) {
        return getType(uri?.toString(), ContentType.XML) as GPathResult
    }

    public GPathResult getHTML(Object uri) {
        return getType(uri?.toString(), ContentType.HTML) as GPathResult
    }

    public Map<String, String> getForm(Object uri) {
        return getType(uri?.toString(), ContentType.URLENC) as Map<String, String>
    }

    public Object getJSON(Object uri) {
        return jsonSlurper.parseText(get(uri))
    }

    public List<NameValuePair> getNameValuePairs(Object str, String charset = DEFAULT_CHARSET) {
        // introduced in HttpClient 4.2
        return URLEncodedUtils.parse(str?.toString(), Charset.forName(charset))
    }

    public List<NameValuePair> getNameValuePairs(URI uri, String charset = DEFAULT_CHARSET) {
        return URLEncodedUtils.parse(uri, charset)
    }

    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    private Object getType(String uri, ContentType contentType) {
        // allocate one per request: try to avoid this exception:
        // java.lang.IllegalStateException: Invalid use of SingleClientConnManager: connection still allocated.
        def http = new HTTPBuilder()

        http.request(uri, GET, contentType) { req ->
            // HTTPBuilder cleans up the reader after this closure, so drain it before returning text
            response.success = { resp, result -> contentType == ContentType.TEXT ? result.getText() : result }
            response.failure = { null } // parity (for now) with LWP::Simple
        }
    }

    // TODO: return a Map on success (ignore headers with multiple values?)
    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    public boolean head(Object uri) {
        def http = new HTTPBuilder()

        http.request(uri?.toString(), HEAD, ContentType.TEXT) { req ->
            response.success = { true }
            response.failure = { false }
        }
    }

    @groovy.transform.CompileStatic(groovy.transform.TypeCheckingMode.SKIP)
    public String target(Object uri) {
        def http = new HTTPBuilder()

        http.request(uri?.toString(), HEAD, ContentType.TEXT) { req ->
            response.success = { resp ->
                getTargetURI(resp.getContext())
            }
            response.failure = { null }
        }
    }

    // XXX wtf?! all this to get the destination URI
    // http://hc.apache.org/httpcomponents-client-dev/tutorial/html/httpagent.html#d4e1195
    private String getTargetURI(HttpContext cxt) {
        def hostURI = (cxt.getAttribute(ExecutionContext.HTTP_TARGET_HOST) as HttpHost).toURI()
        def finalRequest = cxt.getAttribute(ExecutionContext.HTTP_REQUEST) as HttpUriRequest
        def targetURI = null

        try {
            def hostURL = new URI(hostURI).toURL()
            targetURI = new URL(hostURL, finalRequest.getURI().toString()).toExternalForm()
        } catch (Exception e) {
            logger.error("can't determine target URI: $e")
        }

        return targetURI
    }
}
