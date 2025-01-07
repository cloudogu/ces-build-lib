package com.cloudogu.ces.cesbuildlib

class TrivyScanStrategy {
    /**
     * Strategy: Fail if any vulnerability was found.
     */
    static String FAIL = "fail"

    /**
     * Strategy: Make build unstable if any vulnerability was found.
     */
    static String UNSTABLE = "unstable"

    /**
     * Strategy: Ignore any found vulnerability.
     */
    static String IGNORE = "ignore"
}
