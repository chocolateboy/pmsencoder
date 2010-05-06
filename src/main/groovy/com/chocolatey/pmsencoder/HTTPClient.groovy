package com.chocolatey.pmsencoder

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.ParserRegistry
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.HEAD

// there's too much delegation magic for Groovy++ to handle this (currently)
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
