package com.cloudogu.ces.cesbuildlib

class Markdown implements Serializable {
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
        def exitCode
        this.docker.image("ghcr.io/tcort/markdown-link-check:${this.tag}")
            .mountJenkinsUser()
            .inside("--entrypoint=\"\" -v ${this.script.env.WORKSPACE}/docs:/docs") {
                exitCode = this.script.sh(returnStatus: true,
                    script: 'find /docs -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v') as int
            }
        // xargs exits 123 if any markdown-link-check found dead links
        if (exitCode == 123) {
            this.script.unstable("Found offline Markdown links!")
        } else if (exitCode != 0) {
            this.script.error("Markdown link check failed with exit code ${exitCode}")
        }
    }

}
