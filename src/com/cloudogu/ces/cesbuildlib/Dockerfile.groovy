package com.cloudogu.ces.cesbuildlib

class Dockerfile {
    private script

    Dockerfile(script) {
        this.script = script
    }

    /**
     * Lints the Dockerfile with the latest version of hadolint
     */
    void lint(){
        script.docker.image('hadolint/hadolint:latest-debian').inside(){
            script.sh "hadolint Dockerfile"
        }
    }
}
