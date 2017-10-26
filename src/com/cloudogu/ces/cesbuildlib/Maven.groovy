package com.cloudogu.ces.cesbuildlib

abstract class Maven implements Serializable {
    def script

    // Args added to each mvn call
    String additionalArgs = ""

    Maven(script) {
        this.script = script
    }

    def call(args) {
        mvn(args)
    }

    abstract def mvn(String args)

    String createCommandLineArgs(String args) {
        // Apache Maven related side notes:
        // --batch-mode : recommended in CI to inform maven to not run in interactive mode (less logs)
        // -V : strongly recommended in CI, will display the JDK and Maven versions in use.
        //      Very useful to be quickly sure the selected versions were the ones you think.
        // -U : force maven to update snapshots each time (default : once an hour, makes no sense in CI).
        // -Dsurefire.useFile=false : useful in CI. Displays test errors in the logs directly (instead of
        //                            having to crawl the workspace files to see the cause).

        "--batch-mode -V -U -e -Dsurefire.useFile=false ${args + " " + additionalArgs}"
    }

    String getVersion() {
        def matcher = script.readFile('pom.xml') =~ '<version>(.+?)</version>'
        matcher ? matcher[0][1] : ""
    }

    String getMavenProperty(String propertyKey) {
        // Match multi line = (?s)
        def matcher = script.readFile('pom.xml') =~ "(?s)<properties>.*<$propertyKey>(.+)</$propertyKey>.*</properties>"
        matcher ? matcher[0][1] : ""
    }
}