package com.cloudogu.ces.cesbuildlib
/**
 * Run maven in a docker container.
 *
 * This can be helpful,
 * * when constant ports are bound during the build that cause port conflicts in concurrent builds.
 *   For example, when running integration tests, unit tests that use infrastructure that binds to ports or
 * * when one maven repo per builds is required
 *   For example when concurrent builds of multi module project install the same snapshot versions.
 *
 * The build are run inside the official maven containers from https://hub.docker.com/_/maven/
 */
class MavenInDocker extends MavenInDockerBase {

    /** The version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8} **/
    String mavenImage

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param mavenImage the version of the maven docker image to use, e.g. {@code 3.5.0-jdk-8}
     */
    MavenInDocker(script, String mavenImage, String credentialsId = null) {
        super(script)
        this.mavenImage = mavenImage
        this.credentialsId = credentialsId
    }

    @Override
    def call(Closure closure, boolean printStdOut) {
        inDocker(getMavenImage()) {
            sh("mvn ${createCommandLineArgs(closure.call())}", printStdOut)
        }
    }

    //allowing downward compatibility for the old workflow only specifying the tag
    def getMavenImage() {
        return mavenImage.contains(':') ? mavenImage : "maven:${mavenImage}"
    }
}
