package com.cloudogu.ces.cesbuildlib

class Maven implements Serializable {
    def mvnHome
    def javaHome
    def script

    // Args added to each mvn call
    String additionalArgs = ""

    Maven(script, mvnHome, javaHome) {
        this.script = script
        this.mvnHome = mvnHome
        this.javaHome = javaHome
    }

    def call(args) {
        mvn(args)
    }

    def mvn(String args) {
        // Apache Maven related side notes:
        // --batch-mode : recommended in CI to inform maven to not run in interactive mode (less logs)
        // -V : strongly recommended in CI, will display the JDK and Maven versions in use.
        //      Very useful to be quickly sure the selected versions were the ones you think.
        // -U : force maven to update snapshots each time (default : once an hour, makes no sense in CI).
        // -Dsurefire.useFile=false : useful in CI. Displays test errors in the logs directly (instead of
        //                            having to crawl the workspace files to see the cause).

        // Advice: don't define M2_HOME in general. Maven will autodetect its root fine.
        script.withEnv(["JAVA_HOME=${javaHome}", "PATH+MAVEN=${mvnHome}/bin:${script.env.JAVA_HOME}/bin"]) {
            script.sh "${mvnHome}/bin/mvn --batch-mode -V -U -e -Dsurefire.useFile=false ${args + " " + additionalArgs}"
        }
    }

    String getVersion() {
        def matcher = script.readFile('pom.xml') =~ '<version>(.+)</version>'
        matcher ? matcher[0][1] : ""
    }

    String getMavenProperty(String propertyKey) {
        def matcher = script.readFile('pom.xml') =~ "<properties>.*<$propertyKey>(.+)</$propertyKey>.*</properties>"
        matcher ? matcher[0][1] : ""
    }
}