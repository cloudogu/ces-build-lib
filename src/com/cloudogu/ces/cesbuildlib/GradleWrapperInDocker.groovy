package com.cloudogu.ces.cesbuildlib

/**
 * Run gradle using a Gradle Wrapper from the local repository within a Docker Container.
 * The image of the container can be used to specified a JDK.
 */
class GradleWrapperInDocker extends GradleInDockerBase {
    /** The docker image to use, e.g. {@code adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine} **/
    private String imageId

    GradleWrapperInDocker(script, String imageId, String credentialsId = null) {
        super(script)
        this.imageId = imageId
        super.credentialsId = credentialsId
    }

    @Override
    def call(Closure closure, boolean printStdOut) {
        inDocker(imageId) {
            gradlew(closure.call(), printStdOut)
        }
    }

}
