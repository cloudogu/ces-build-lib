package com.cloudogu.ces.cesbuildlib

class Markdown implements Serializable{
    Sh sh
    private script
    Docker docker

    Markdown(script) {
        this.sh = new Sh(script)
    }

    def check(){
        docker.image("ghcr.io/tcort/markdown-link-check:stable")
            .mountJenkinsUser()
            .inside('--entrypoint="" -v ${WORKSPACE}/docs:/tmp') {
                script.sh 'find /tmp -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v'
            }
    }

}
