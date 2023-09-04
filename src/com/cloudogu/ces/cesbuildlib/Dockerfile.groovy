package com.cloudogu.ces.cesbuildlib

class Dockerfile {
    private script

    Dockerfile(script) {
        this.script = script
    }

    /**
     * Lints the Dockerfile with hadolint using a configuration file
     *
     * To configure hadelint, add a ".hadolint.yaml" file to your working directory
     * See https://github.com/hadolint/hadolint#configure
     *
     * @param dockerfile Path to the Dockerfile that should be linted
     * @param configuration Path to the hadolint configuration file
     * @param hadolintVersion Version of the hadolint/hadolint container image
     */
    void lintWithConfig(String dockerfile = "Dockerfile", String configuration = ".hadolint.yaml", hadolintVersion = "latest-debian"){
        script.docker.image("hadolint/hadolint:${hadolintVersion}").inside(){
            script.sh "hadolint --no-color -c ${configuration} ${dockerfile}"
        }
    }

    /**
     * Lints the Dockerfile with the latest version of hadolint
     * Only fails on errors, ignores warnings etc.
     * Trusts registries docker.io, gcr.io and registry.cloudogu.com
     *
     * @param dockerfile Path to the Dockerfile that should be linted
     * @param hadolintVersion Version of the hadolint/hadolint container image
     */
    void lint(String dockerfile = "Dockerfile", hadolintVersion = "latest-debian"){
        script.docker.image("hadolint/hadolint:${hadolintVersion}").inside(){
            script.sh "hadolint -t error --no-color --trusted-registry docker.io --trusted-registry gcr.io --trusted-registry registry.cloudogu.com ${dockerfile}"
        }
    }
}
