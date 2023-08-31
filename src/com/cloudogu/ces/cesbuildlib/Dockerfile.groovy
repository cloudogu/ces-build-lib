package com.cloudogu.ces.cesbuildlib

class Dockerfile {
    private script

    Dockerfile(script) {
        this.script = script
    }

    /**
     * Lints the Dockerfile with the latest version of hadolint
     *
     * To configure hadelint, add a ".hadolint.yaml" file to your working directory
     * See https://github.com/hadolint/hadolint#configure
     */
    void lint(String dockerfile = "Dockerfile"){
        script.docker.image('hadolint/hadolint:latest-debian').inside(){
            script.sh "hadolint ${dockerfile}"
        }
    }

    /**
     * Lints the Dockerfile with the latest version of hadolint
     * Only fails on errors, ignores warnings etc.
     * Trusts registries docker.io, gcr.io and registry.cloudogu.com
     */
    void lintDefault(String dockerfile = "Dockerfile"){
        script.docker.image('hadolint/hadolint:latest-debian').inside(){
            script.sh "hadolint -t error --no-color --trusted-registry docker.io --trusted-registry gcr.io --trusted-registry registry.cloudogu.com ${dockerfile}"
        }
    }
}
