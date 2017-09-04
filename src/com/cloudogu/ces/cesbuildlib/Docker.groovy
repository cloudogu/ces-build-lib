package com.cloudogu.ces.cesbuildlib

/**
 * Basic abstraction for docker.
 *
 * Provides all methods of the global docker-variable (see https://<jenkinsUlr>/job/<jobname>/pipeline-syntax/globals#docker)
 * as well as some convenience methods.
 */
class Docker implements Serializable {
    def script
    Sh sh

    Docker(script) {
        this.script = script
        this.sh = new Sh(script)
    }

    /* Define methods of global docker variable again.
       Using methodMissing(String name, args) is not possible, because we would need the spread operator for dynamic
       arguments (*) which is not supported by Jenkins Pipeline

        def methodMissing(String name, args) {
            return this.script.docker."$name"(*args)
        }

       Leads to UnsupportedOperationException.

       Instead ust delegate all methods manually. See
       https://github.com/jenkinsci/docker-workflow-plugin/blob/master/src/main/resources/org/jenkinsci/plugins/docker/workflow/Docker.groovy
     */

    /**
     * Specifies a registry URL such as https://docker.mycorp.com/, plus an optional credentials ID to connect to it.
     *
     * Example:
     * <pre>
     *   def dockerImage = docker.build("image/name:1.0", "folderOfDockfile")
     *   docker.withRegistry("https://your.registry", 'credentialsId') {
     *       dockerImage.push()
     *   }
     *  </pre>
     */
    def withRegistry(String url, String credentialsId = null, Closure body) {
        return this.script.docker.withRegistry( url, credentialsId, body)
    }

    /**
     * Specifies a server URI such as tcp://swarm.mycorp.com:2376, plus an optional credentials ID to connect to it.
     */
    def withServer(String uri, String credentialsId = null, Closure body) {
        return this.script.docker.withServer(uri, credentialsId, body)
    }

    /**
     * Specifies the name of a Docker installation to use, if any are defined in Jenkins global configuration.
     * If unspecified, docker is assumed to be in the $PATH of the Jenkins agent.
     */
    def withTool(String toolName, Closure body) {
        return this.script.docker.withTool(toolName, body)
    }

    /**
     * Creates an Image object with a specified name or ID.
     *
     * Example:
     * <pre>
     *      docker.image('google/cloud-sdk:164.0.0').inside("-e HOME=${pwd()}") {
     *          sh "echo something"
     *      }
     *  </pre>
     */
    def image(String id) {
        return this.script.docker.image(id)
    }

    /**
     * Runs docker build to create and tag the specified image from a Dockerfile in the current directory.
     * Additional args may be added, such as '-f Dockerfile.other --pull --build-arg http_proxy=http://192.168.1.1:3128 .'.
     * Like docker build, args must end with the build context.
     *
     * Example:
     * <pre>
     *   def dockerContainer = docker.build("image/name:1.0", "folderOfDockfile").run("-e HOME=${pwd()}")
     *  </pre>
     *
     * @return the resulting Image object. Records a FROM fingerprint in the build
     */
    def build(String image, String args = '.') {
        return this.script.docker.build(image, args)
    }

    /**
     * @param container docker container instance
     * @return the IP address for a docker container instance
     */
    String findIp(container) {
        sh.returnStdOut "docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${container.id}"
    }

    /**
     * @param container docker container instance
     * @return the environment variables set within the docker container as string
     */
    String findEnv(container) {
        sh.returnStdOut "docker exec ${container.id} env"
    }

    boolean isRunning(container) {
        return Boolean.valueOf(sh.returnStdOut("docker inspect -f {{.State.Running}} ${container.id}"))
    }
}