@Typed(TypePolicy.DYNAMIC)

/*
    XXX This needs to be dynamic for now, due to a bug in the MIXED
    typing, which causes head to return true instead of false, with
    hilarious consequences

    see src/tests/groovy/HTTPClientTest.groovy
*/

package com.chocolatey.pmsencoder

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ParserRegistry
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.HEAD

class HTTPClient {
    private HTTPBuilder http = new HTTPBuilder()

    String get(String uri) {
        http.request(uri, GET, TEXT) { req ->
            response.success = { resp, reader -> reader.getText() }
            response.failure = { null } // parity (for now) with LWP::Simple
        }
    }

    boolean head(String uri) {
        http.request(uri, HEAD, TEXT) { req ->
            response.success = { true }
            response.failure = { false }
        }
    }
}
