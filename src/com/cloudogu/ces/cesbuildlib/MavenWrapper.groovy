package com.cloudogu.ces.cesbuildlib

/**
 * Run maven using a Maven Wrapper from the local repository.
 *
 * See https://github.com/takari/maven-wrapper
 */
class MavenWrapper extends Maven {

    def javaHome

    MavenWrapper(script, javaHome = null) {
        super(script)
        this.javaHome = javaHome
    }

    @Override
    def mvn(String args) {

        if (javaHome) {
            // PATH+something prepends to PATH
            script.withEnv(["JAVA_HOME=${javaHome}", "PATH+JDK=${script.env.JAVA_HOME}/bin"]) {
                mvnw(args)
            }
        } else {
            mvnw(args)
        }
    }

    def mvnw(String args) {
        script.sh "./mvnw ${createCommandLineArgs(args)}"
    }
}