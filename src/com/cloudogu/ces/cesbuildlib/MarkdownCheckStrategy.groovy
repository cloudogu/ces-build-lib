package com.cloudogu.ces.cesbuildlib

class MarkdownCheckStrategy {
    /**
     * Strategy: Fail if any offline link was found.
     */
    static String FAIL = "fail"

    /**
     * Strategy: Make build unstable if any offline link was found.
     */
    static String UNSTABLE = "unstable"
}
