package com.cloudogu.ces.cesbuildlib

class Gpg {
    private script
    private docker

    Gpg(script, docker) {
        this.script = script
        this.docker = docker
    }

    /**
     * Executes the 'make signature-ci' command to create the digital signature for a build.
     */
    void createSignature() {
        this.withPrivateKey {
            // The passphrase variable is provided in `withPrivateKey`
            script.sh("make passphrase=\$passphrase signature-ci")
        }
    }

    /**
     * Calls a closure inside of a docker container which is able to execute 'gpg' commands as well as 'make' commands.
     * @param closure The closure which is called inside of the docker container.
     */
    private void withGpg(Closure closure) {
        this.buildGpgDockerImage()
        String dockerArgs = "-v ${script.env.WORKSPACE}:/tmp/workspace"
        dockerArgs <<= " --entrypoint=''"
        dockerArgs <<= " -v ${script.env.pwd}/.gnupg:/root/.gnupg"
        docker
            .image('cloudogu/gpg:1.0')
            .mountJenkinsUser()
            .inside(dockerArgs) {
                script.sh "cd /tmp/workspace"
                closure.call()
            }
    }

    /**
     * Creates, builds and then removes a Dockerfile which is able to execute 'gpg' commands as well as 'make' commands.
     */
    private void buildGpgDockerImage() {
        def dockerfile = """
        FROM debian:stable-slim
        LABEL maintainer="hello@cloudogu.com"

        RUN apt update && apt install -y gnupg2 make git

        ENTRYPOINT ["/usr/bin/gpg"]
    """
        try {
            script.writeFile encoding: 'UTF-8', file: 'Dockerfile.gpgbuild', text: dockerfile.trim()
            docker.build("cloudogu/gpg:1.0", "-f Dockerfile.gpgbuild .")
        } catch (e) {
            script.echo "${e}"
            throw e
        } finally {
            script.sh "rm -f Dockerfile.gpgbuild"
        }
    }

    /**
     * Calls a closure in an environment which contains a gpg signing key and is able to execute 'gpg' commands and 'make' commands.
     * @param closure The closure to call.
     */
    private void withPrivateKey(Closure closure) {
        script.withCredentials([script.string(credentialsId: 'jenkins_gpg_private_key_passphrase', variable: 'passphrase')]) {
            script.withCredentials([script.file(credentialsId: 'jenkins_gpg_private_key_for_ces_tool_release_signing', variable: 'pkey')]) {
                try {
                    withGpg {
                        script.sh "gpg --yes --always-trust --pinentry-mode loopback --passphrase=\"\$passphrase\" --import \$pkey"
                        closure.call()
                    }
                } catch (e) {
                    script.echo "${e}"
                    throw e
                }
                finally {
                    script.sh "rm -f \$pkey"
                    script.sh "rm -rf .gnupg"
                }
            }
        }
    }
}
