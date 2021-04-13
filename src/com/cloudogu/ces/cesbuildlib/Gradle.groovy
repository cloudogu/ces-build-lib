package com.cloudogu.ces.cesbuildlib

abstract class Gradle implements Serializable {
    protected script

    Gradle(script) {
        this.script = script
    }

    def call(String args, boolean printStdOut = true) {
        gradle(args, printStdOut)
    }

    /**
     * @param printStdOut - returns output of gradle as String instead of printing to console
     */
    protected abstract def gradle(String args, boolean printStdOut = true)

    def gradlew(String args, boolean printStdOut) {
        sh("./gradlew -q "+ args, printStdOut)
    }

    void sh(String command, boolean printStdOut) {
        script.echo "executing sh: ${command}, return Stdout: ${printStdOut}"
        if (printStdOut) {
            // -V : strongly recommended in CI, will display the JDK and Maven versions in use.
            // Don't use this with sh(returnStdout: true ..) !
            script.sh "${command} -V"
        } else {
            new Sh(script).returnStdOut command
        }
    }
}
