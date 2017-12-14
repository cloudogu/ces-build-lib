package com.cloudogu.ces.cesbuildlib

/**
 * An abstraction for the {@code sh} step
 */
class Sh implements Serializable {
    def script

    Sh(script) {
        this.script = script
    }

    /**
     * @return the trimmed stdout of the shell call. Most likeley never {@code null}
     */
    String returnStdOut(args) {
        return script.sh(returnStdout: true, script: args)
                // Trim to remove trailing line breaks, which result in unwanted behavior in Jenkinsfiles:
                // E.g. when using output in other sh() calls leading to executing the sh command after the line breaks,
                // possibly discarding additional arguments
                .trim()
    }
}