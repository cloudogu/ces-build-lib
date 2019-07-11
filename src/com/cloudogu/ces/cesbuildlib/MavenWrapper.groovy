package com.cloudogu.ces.cesbuildlib

/**
 * Run maven using a Maven Wrapper from the local repository.
 *
 * See https://github.com/takari/maven-wrapper
 */
class MavenWrapper extends Maven {

    private javaHome

    /**
     * @deprecated
     * Using no explicit Java tool results in using the one that happens to be in the PATH of the build agent.
     * Experience tells us that this is absolutely non-deterministic and will result in unpredictable behavior.
     * So: Better set an explicit Java tool to be used, or use MavenWrapperInDocker.
     *
     */
    @Deprecated
    MavenWrapper(script) {
        this(script, '')
    }

    MavenWrapper(script, javaHome) {
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
}