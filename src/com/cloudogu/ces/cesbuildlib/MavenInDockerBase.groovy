package com.cloudogu.ces.cesbuildlib
/**
 * Common tools for all MavenInDocker classes
 */
abstract class MavenInDockerBase extends Maven {

    /** Setting this to {@code true} allows the maven build to access the docker host, i.e. to start other containers.*/
    boolean enableDockerHost = false

    /** Setting this to {@code true} makes Maven use Jenkin's local maven repo instead of one in the build's workspace
     * Using the Jenkins speeds up the first build and uses less memory. However, concurrent builds of multi module
     * projects building the same version (e.g. a SNAPSHOT), might overwrite their dependencies, causing
     * non-deterministic build failures.*/
    boolean useLocalRepoFromJenkins = false

    Docker docker

    MavenInDockerBase(script) {
        super(script)
        this.docker = new Docker(script)
    }

    @Override
    def mvn(String args) {
        call {
            args
        }
    }

    abstract def call(Closure closure);

    String createDockerRunArgs() {
        String runArgs = ""

        if (useLocalRepoFromJenkins) {
            // If Jenkin's local maven repo does not exist, make sure it is created by the user that runs the build.
            // Otherwise, if not existing, this folder is create as root, which denies permission to jenkins
            script.sh returnStatus: true, script: 'mkdir -p $HOME/.m2'

            // Mount Jenkin's local maven repo as local maven repo within the container
            runArgs += " -v ${script.env.HOME}/.m2:${script.pwd()}/.m2"
        }

        return runArgs
    }

    protected void inDocker(String imageId, Closure closure) {
        docker.image(imageId)
        // Mount user and set HOME, which results in the workspace being user.home. Otherwise '?' might be the user.home.
                .mountJenkinsUser(true)
                .mountDockerSocket(enableDockerHost)
                .inside(createDockerRunArgs()) {
                    closure.call()
                }
    }

}
