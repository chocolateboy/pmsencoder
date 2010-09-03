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
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.HEAD

class HTTPClient {
    private HTTPBuilder http = new HTTPBuilder()

    @Typed(TypePolicy.DYNAMIC)
    String get(String uri) {
        http.request(uri, GET, TEXT) { req ->
            response.success = { resp, reader -> reader.getText() }
            response.failure = { null } // parity (for now) with LWP::Simple
        }
    }

    @Typed(TypePolicy.MIXED)
    boolean head(String uri) {
        http.request(uri, HEAD, TEXT) { req ->
            response.success = { true }
            response.failure = { false }
        }
    }

    @Typed(TypePolicy.MIXED)
    String mixed_get(String uri) {
        http.request(uri, GET, TEXT) { req ->
            response.success = { resp, reader -> reader.getText() }
            response.failure = { null } // parity (for now) with LWP::Simple
        }
    }
}
