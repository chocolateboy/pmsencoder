@Typed
package com.chocolatey.pmsencoder

// if these extend Exception (rather than RuntimeException) Groovy(++?) wraps them in
// InvokerInvocationException, which causes all kinds of tedium.
// For the time being: extend RuntimeException - even though they're both checked

public class MatchFailureException extends RuntimeException { }

public class PMSEncoderException extends RuntimeException {
    // grrr: get "cannot find constructor" errors without this (another GString issue?)
    PMSEncoderException(String msg) {
        super(msg)
    }
}

