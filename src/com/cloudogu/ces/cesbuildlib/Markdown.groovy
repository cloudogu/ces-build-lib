package com.cloudogu.ces.cesbuildlib

class Markdown implements Serializable{
    Sh sh
    private script
    Docker docker

    Markdown(script) {
        this.script = script
        this.sh = new Sh(script)
        this.docker = new Docker(script)
    }

    def check(){
        this.docker.image("ghcr.io/tcort/markdown-link-check:stable")
            .mountJenkinsUser()
            .inside("--entrypoint=\"\" -v ${this.script.env.WORKSPACE}/docs:/docs") {
                this.script.sh 'find /docs -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v'
            }
    }

}
