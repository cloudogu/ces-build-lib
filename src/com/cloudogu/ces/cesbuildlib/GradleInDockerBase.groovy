package com.cloudogu.ces.cesbuildlib
/**
 * Common tools for all GradleInDocker classes
 */
abstract class GradleInDockerBase extends Gradle {

    /** Setting this to {@code true} allows the Gradle build to access the docker host, i.e. to start other containers.*/
    boolean enableDockerHost = false
    protected String credentialsId = null

    Docker docker

    GradleInDockerBase(script) {
        super(script)
        this.docker = new Docker(script)
    }

    @Override
    def gradle(String args, boolean printStdOut = true) {
        call({ args }, printStdOut)
    }

    abstract def call(Closure closure, boolean printStdOut);

    protected void inDocker(String imageId, Closure closure) {
        if (this.credentialsId) {
            docker.withRegistry("https://${imageId}", this.credentialsId) {
                dockerImageBuilder(imageId, closure)
            }
        } else {
            dockerImageBuilder(imageId, closure)
        }
    }

    protected void dockerImageBuilder(String imageId , closure) {
        docker.image(imageId)
        // Mount user and set HOME, which results in the workspace being user.home. Otherwise '?' might be the user.home.
            .mountJenkinsUser(true)
            .mountDockerSocket(enableDockerHost)
            .inside("") {
                closure.call()
            }
    }

}
