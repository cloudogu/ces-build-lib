package com.cloudogu.ces.cesbuildlib

/**
 * Run maven from a local tool installation on Jenkins.
 */
class MavenLocal extends Maven {
    def mvnHome
    def javaHome

    MavenLocal(script, mvnHome, javaHome) {
        super(script)
        this.mvnHome = mvnHome
        this.javaHome = javaHome
    }

    @Override
    def mvn(String args) {
        // Advice: don't define M2_HOME in general. Maven will autodetect its root fine.
        script.withEnv(["JAVA_HOME=${javaHome}", "PATH+MAVEN=${mvnHome}/bin:${script.env.JAVA_HOME}/bin"]) {
            script.sh "${mvnHome}/bin/mvn ${createCommandLineArgs(args)}"
        }
    }
}