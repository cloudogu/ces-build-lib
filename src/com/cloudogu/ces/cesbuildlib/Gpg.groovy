package com.cloudogu.ces.cesbuildlib

class Gpg {
    private script

    Gpg(script) {
        this.script = script
    }

    public void createSignature(){
        this.withPrivateKey {
            script.sh ("make passphrase=${passphrase} signature-ci") // The passphrase variable is provided in `withPrivateKey`
        }
    }

    private void withGpg(Closure closure){
        withImage{
            String dockerArgs = "-v ${WORKSPACE}:/tmp/workspace"
            dockerArgs <<= " --entrypoint=''"
            dockerArgs <<= " -v ${pwd}/.gnupg:/root/.gnupg"
            new Docker(this)
                .image('cloudogu/gpg:1.0')
                .mountJenkinsUser()
                .inside(dockerArgs){
                    script.sh "cd /tmp/workspace"
                    closure.call()
                }
        }
    }

    private void withImage(Closure closure){
        dockerfile = """
        FROM debian:stable-slim
        LABEL maintainer="simon.klein@cloudogu.com"

        RUN apt update && apt install -y gnupg2 make git

        ENTRYPOINT ["/usr/bin/gpg"]
    """
        try {
            script.writeFile encoding: 'UTF-8', file: 'Dockerfile.gpgbuild', text: dockerfile.trim()
            new Docker(this).build("cloudogu/gpg:1.0", "-f Dockerfile.gpgbuild .")
            closure.call()
        } catch(e){
            script.echo "${e}"
            throw e
        } finally {
            script.sh "rm -f Dockerfile.gpgbuild"
        }
    }

    private void withPrivateKey(Closure closure){
        script.withCredentials([script.string(credentialsId: 'jenkins_gpg_private_key_passphrase', variable: 'passphrase')]) {
            script.withCredentials([script.file(credentialsId: 'jenkins_gpg_private_key_for_ces_tool_release_signing', variable: 'pkey')]) {
                try {
                    withGpg {
                        script.sh "gpg --yes --always-trust --pinentry-mode loopback --passphrase=\"\$passphrase\" --import \$pkey"
                        closure.call()
                    }
                } catch(e){
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
