/*
    XXX get and head need to be dynamic for now, due to a bug in the MIXED
    typing, which causes head to return true instead of false, with
    hilarious consequences

    mixed_head and_mixed_get are included so that the test suite can check to
    see if a fix has been committed

    see src/test/groovy/HTTPClientTest.groovy
*/

@Typed
package com.chocolatey.pmsencoder

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ParserRegistry

import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.HttpHost
import org.apache.http.protocol.ExecutionContext
import org.apache.http.protocol.HttpContext

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.HEAD

class HTTPClient extends Logger {
    private HTTPBuilder http = new HTTPBuilder()

    @Typed(TypePolicy.MIXED)
    public String get(String uri) {
        http.request(uri, GET, TEXT) { req ->
            response.success = { resp, reader -> reader.getText() }
            response.failure = { null } // parity (for now) with LWP::Simple
        }
    }

    @Typed(TypePolicy.MIXED)
    public boolean head(String uri) {
        http.request(uri, HEAD, TEXT) { req ->
            response.success = { true }
            response.failure = { false }
        }
    }

    @Typed(TypePolicy.MIXED)
    public String target(String uri) {
        http.request(uri, HEAD, TEXT) { req ->
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
        def targetURI

        try {
            def hostURL = new URI(hostURI).toURL()
            targetURI = new URL(hostURL, finalRequest.getURI().toString()).toExternalForm()
        } catch (Exception e) {
            log.error("can't determine target URI: $e")
        }

        return targetURI
    }
}
