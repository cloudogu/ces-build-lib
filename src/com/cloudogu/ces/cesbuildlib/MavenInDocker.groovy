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
class MavenInDocker extends Maven {

    /** The version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8} **/
    String dockerBaseImageVersion

    /** Setting this to {@code true} allows the maven build to access the docker host, i.e. to start other containers.*/
    boolean enableDockerHost = false

    /** Setting this to {@code true} makes Maven use Jenkin's local maven repo instead of one in the build's workspace
     * Using the Jenkins speeds up the first build and uses less memory. However, concurrent builds of multi module
     * projects building the same version (e.g. a SNAPSHOT), might overwrite their dependencies, causing
     * non-deterministic build failures.*/
    boolean useLocalRepoFromJenkins = false

    Docker docker

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param dockerBaseImageVersion the version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8}
     */
    MavenInDocker(script, String dockerBaseImageVersion) {
        super(script)
        this.dockerBaseImageVersion = dockerBaseImageVersion
        this.docker = new Docker(script)
    }

    @Override
    def mvn(String args) {

        docker.image("maven:$dockerBaseImageVersion")
                // Mount user and set HOME, which results in the workspace being user.home. Otherwise '?' might be the user.home.
                .mountJenkinsUser(true)
                .mountDockerSocket(enableDockerHost)
                .inside(createDockerRunArgs()) {
                    script.sh("mvn ${createCommandLineArgs(args)}")
                }
    }

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
}
