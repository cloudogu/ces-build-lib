package com.cloudogu.ces.cesbuildlib

def call(String dockerfile = "Dockerfile") {
    // only latest version available
    docker.image('projectatomic/dockerfile-lint:latest').inside({
        sh "dockerfile_lint -p -f ${dockerfile}"
    })
}
