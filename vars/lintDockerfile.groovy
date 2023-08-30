package com.cloudogu.ces.cesbuildlib

def call(String dockerfile = "Dockerfile") {
    docker.image('hadolint/hadolint:latest-debian').inside(){
        sh "hadolint --no-color -t error " +
            "--trusted-registry docker.io --trusted-registry gcr.io --trusted-registry registry.cloudogu.com " +
            "${WORKSPACE}/${dockerfile}"
    }
}
