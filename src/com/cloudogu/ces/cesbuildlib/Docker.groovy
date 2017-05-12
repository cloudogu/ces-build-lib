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

    def methodMissing(String name, args) {
        // TODO delegate missingMethods to this.script.docker
        //this.script.docker
        return this.script.docker."$name"(*args)
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