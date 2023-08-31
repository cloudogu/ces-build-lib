package com.cloudogu.ces.cesbuildlib

class Dockerfile {
    private script
    private Sh sh

    Dockerfile(script) {
        this.script = script
        this.sh = new Sh(script)
    }

    /**
     * Lints the Dockerfile with the latest version of hadolint
     */
    void lint(){
        docker.image('hadolint/hadolint:latest-debian').inside(){
            sh "hadolint Dockerfile"
        }
    }
}
