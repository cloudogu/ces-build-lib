package com.cloudogu.ces.cesbuildlib

/**
 * Run maven using a Maven Wrapper from the local repository within a Docker Container.
 * The image of the container can be used to specified a JDK.
 *
 * See https://github.com/takari/maven-wrapper
 */
class MavenWrapperInDocker extends MavenInDockerBase {

    /** The docker image to use, e.g. {@code adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine} **/
    private String imageId

    @SuppressWarnings("GrDeprecatedAPIUsage") // MavenWrapper will become protected constructor that is no longer deprecated
    MavenWrapperInDocker(script, String imageId) {
        super(script)
        this.imageId = imageId
    }

    @Override
    def call(Closure closure) {
        inDocker(imageId) {
            mvnw(closure.call())
        }
    }
}