package com.cloudogu.ces.cesbuildlib

/**
 * This exception is thrown whenever a vulnerability was found.
 */
class TrivyScanException extends RuntimeException {
    TrivyScanException(String error) {
        super(error)
    }
}
