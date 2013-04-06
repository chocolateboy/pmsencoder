@Typed
package com.chocolatey.pmsencoder

import groovy.util.slurpersupport.GPathResult

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ParserRegistry

import net.sf.json.JSON

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

class HTTPClient implements LoggerMixin {
    public String get(String uri) {
        getType(uri, ContentType.TEXT)
    }

    public GPathResult getXML(String uri) {
        return getType(uri, ContentType.XML)
    }

    public GPathResult getHTML(String uri) {
        return getType(uri, ContentType.HTML)
    }

    public Map<String,String> getForm(String uri) {
        return getType(uri, ContentType.URLENC)
    }

    public JSON getJSON(String uri) {
        return getType(uri, ContentType.JSON)
    }

    public List<NameValuePair> getNameValuePairs(String str) {
        // introduced in HttpClient 4.2
        return URLEncodedUtils.parse(str, Charset.forName('UTF-8'))
    }

    public List<NameValuePair> getNameValuePairs(URI uri) {
        return URLEncodedUtils.parse(uri, 'UTF-8')
    }

    @Typed(TypePolicy.MIXED)
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
    @Typed(TypePolicy.MIXED)
    public boolean head(String uri) {
        def http = new HTTPBuilder()
        http.request(uri, HEAD, ContentType.TEXT) { req ->
            response.success = { true }
            response.failure = { false }
        }
    }

    @Typed(TypePolicy.MIXED)
    public String target(String uri) {
        def http = new HTTPBuilder()
        http.request(uri, HEAD, ContentType.TEXT) { req ->
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
            log.error("can't determine target URI: $e")
        }

        return targetURI
    }
}
