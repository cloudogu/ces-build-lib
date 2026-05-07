package com.cloudogu.ces.cesbuildlib

/**
 * Run maven using a Maven Wrapper from the local repository within a Docker Container.
 * The image of the container can be used to specified a JDK.
 *
 * See https://github.com/takari/maven-wrapper
 */
class MavenWrapperInDocker extends MavenInDockerBase {

    /** The docker image to use, e.g. {@code adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine} **/
    private String imageName

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param imageName the version of the maven docker image to use, e.g. {@code 3.5.0-jdk-8}
     * @param registryCredentialsId the registryCredentialsId (From Jenkins) to use for authenticating to the registry, if the mavenImage is not public.
     * @param registryUrl the registryUrl to use for getting the image
     * @param jenkinsCredentialsId the credentialsId (From Jenkins) to use for authenticating to the private nexus repository, if required.
     */
    MavenWrapperInDocker(script, String imageName, String registryCredentialsId = null, String registryUrl = null, String jenkinsCredentialsId = "jenkins") {
        super(script)
        this.imageName = imageName
        this.registryCredentialsId = registryCredentialsId
        this.registryUrl = registryUrl
        this.jenkinsCredentialsId = jenkinsCredentialsId
    }

    @Override
    def call(Closure closure, boolean printStdOut) {
        inDocker(imageName) {
            mvnw(closure.call(), printStdOut)
        }
    }
}
