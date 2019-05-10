package com.cloudogu.ces.cesbuildlib

/**
 * Run maven from a local tool installation on Jenkins.
 */
class MavenLocal extends Maven {
    private mvnHome
    private javaHome

    MavenLocal(script, mvnHome, javaHome) {
        super(script)
        this.mvnHome = mvnHome
        this.javaHome = javaHome
    }

    @Override
    def mvn(String args) {
        // Advice: don't define M2_HOME in general. Maven will autodetect its root fine.
        // PATH+something prepends to PATH
        warnIfToolsNotInstalled()
        script.withEnv(["JAVA_HOME=${javaHome}", "PATH+MAVEN=${mvnHome}/bin:${script.env.JAVA_HOME}/bin"]) {
            script.sh "${mvnHome}/bin/mvn ${createCommandLineArgs(args)}"
        }
    }

    void warnIfToolsNotInstalled() {
        /* Unfortunately, Jenkins seems to silently return null when calling "tool 'toolID'" for an existing tool that
          does not support auto installation. */
        if (!mvnHome) {
            script.echo 'WARNING: mvnHome is empty. Did you check "Install automatically"?'
        }
        if (!javaHome) {
            script.echo 'WARNING: javaHome is empty. Did you check "Install automatically"?'
        }
    }
}