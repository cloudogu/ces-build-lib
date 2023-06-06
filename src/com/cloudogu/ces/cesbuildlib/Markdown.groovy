package com.cloudogu.ces.cesbuildlib

class Markdown implements Serializable{
    Sh sh
    private script
    Docker docker
    private String tag

    Markdown(script, String tag = "stable") {
        this.script = script
        this.sh = new Sh(script)
        this.docker = new Docker(script)
        this.tag = tag
    }

    def check(){
        this.docker.image("ghcr.io/tcort/markdown-link-check:${this.tag}")
            .mountJenkinsUser()
            .inside("--entrypoint=\"\" -v ${this.script.env.WORKSPACE}/docs:/docs") {
                this.script.sh 'find /docs -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v'
            }
    }

}
