package com.cloudogu.ces.cesbuildlib

/**
 * Basic abstraction for docker.
 *
 * Provides all methods of the global docker-variable (see https://<jenkinsUlr>/job/<jobname>/pipeline-syntax/globals#docker)
 * as well as some convenience methods.
 */
class Docker implements Serializable {
    @Delegate Serializable docker
    def script

    Docker(script) {
        this.script = script
        this.docker = this.script.docker
    }

    def methodMissing(String name, args) {
        // Leads to java.lang.UnsupportedOperationException
        // * - Spread operator for dynamic arguments
        return this.script.docker."$name"(*args)
        // Leads to MissingMethodException: No signature of method: org.jenkinsci.plugins.docker.workflow.Docker.image() is applicable for argument types: ([Ljava.lang.Object;) values: [[postgres:9.6]]
        //return this.script.docker."$name"(args)

        // Does not find all methods
        //this.script.docker.getClass().getMethods().each { method ->
/*        this.script.docker.metaClass.metaMethods.each { method ->
            this.script.echo "${method.name}"
            if (method.name == name) {
                return method.invoke(this.script.docker."$name", args)
            }
        }*/
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