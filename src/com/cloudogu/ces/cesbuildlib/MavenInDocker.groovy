package com.cloudogu.ces.cesbuildlib

/**
 * Run maven in a docker container.
 * This can be helpful, when constants ports are bound during the build and concurrent builds cause port conflicts.
 * For example, when running integration tests.
 *
 * The builds base on the official maven containers from https://hub.docker.com/_/maven/
 */
class MavenInDocker extends Maven {
    String dockerImageVersion

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param dockerImageVersion the version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8}
     */
    MavenInDocker(script, String dockerImageVersion) {
        super(script, null, null)
        this.dockerImageVersion = dockerImageVersion
    }

    @Override
    def mvn(String args) {
        String jenkinsHome = script.env.HOME

        script.withEnv(["HOME=${script.pwd()}"]) {
            script.docker.image("maven:$dockerImageVersion")
                    .inside("--volume=\"${jenkinsHome}/.m2/repository:${script.pwd()}/.m2/repository:rw\"") {
                script.sh "mvn ${createCommandLineArgs(args)} -Duser.home=\"${script.pwd()}\" -s \"${script.pwd()}/.m2/settings.xml\""
            }
        }
    }
}