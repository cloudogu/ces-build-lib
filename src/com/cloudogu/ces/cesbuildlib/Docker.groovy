package com.cloudogu.ces.cesbuildlib
/**
 * Basic abstraction for docker.
 *
 * Provides all methods of the global docker-variable (see https://<jenkinsUlr>/job/<jobname>/pipeline-syntax/globals#docker)
 * as well as some convenience methods.
 */
class Docker implements Serializable {
    def script

    Docker(script) {
        this.script = script
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

    // TODO copy doc from Jenkins and add some examples for calling


    def withRegistry(String url, String credentialsId = null, Closure body) {
        return this.script.docker.withRegistry( url, credentialsId, body)
    }

    def withServer(String uri, String credentialsId = null, Closure body) {
        return this.script.docker.withServer(uri, credentialsId, body)
    }

    def withTool(String toolName, Closure body) {
        return this.script.docker.withTool(toolName, body)
    }

    def image(String id) {
        return this.script.docker.image(id)
    }

    def build(String image, String args = '.') {
        return this.script.docker.build(image, args)
    }

    /**
     * @param container docker container instance
     * @return the IP address for a docker container instance
     */
    // Note this method must not be private, in order to avoid RejectedAccessException: unclassified method java
    String hostIp(container) {
        String ip =
                script.sh(returnStdout: true,
                        script: "docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${container.id}")
                        .trim()
        return ip
    }
}