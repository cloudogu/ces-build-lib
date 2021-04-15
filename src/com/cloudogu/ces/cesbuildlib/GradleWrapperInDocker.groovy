package com.cloudogu.ces.cesbuildlib

/**
 * Run gradle using a Gradle Wrapper from the local repository within a Docker Container.
 * The image of the container can be used to specified a JDK.
 */
class GradleWrapperInDocker extends GradleInDockerBase {
    /** The docker image to use, e.g. {@code adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine} **/
    private String imageId

    @SuppressWarnings("GrDeprecatedAPIUsage") // GradleWrapper will become protected constructor that is no longer deprecated
    GradleWrapperInDocker(script, String imageId) {
        super(script)
        this.imageId = imageId
    }

    @Override
    def call(Closure closure, boolean printStdOut) {
        inDocker(imageId) {
            gradlew(closure.call(), printStdOut)
        }
    }
}
