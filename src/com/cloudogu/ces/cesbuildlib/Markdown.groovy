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

    /**
     * Checks all Markdown files in the docs directory for offline links.
     *
     * @param strategy The strategy to follow if offline links are found. Should the build become unstable or failed? (@see MarkdownCheckStrategy)
     */
    def check(String strategy = MarkdownCheckStrategy.UNSTABLE){
        def exitCode
        this.docker.image("ghcr.io/tcort/markdown-link-check:${this.tag}")
            .mountJenkinsUser()
            .inside("--entrypoint=\"\" -v ${this.script.env.WORKSPACE}/docs:/docs") {
                exitCode = this.script.sh(returnStatus: true,
                    script: 'find /docs -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v') as int
            }
        // xargs exits 123 if any markdown-link-check found dead links
        if (exitCode == 123) {
            switch (strategy) {
                case MarkdownCheckStrategy.UNSTABLE:
                    this.script.unstable("Found offline Markdown links!")
                    break
                case MarkdownCheckStrategy.FAIL:
                    this.script.error("Found offline Markdown links!")
                    break
                default:
                    this.script.error("Unknown Markdown check strategy: ${strategy}")
            }
        } else if (exitCode != 0) {
            this.script.error("Markdown link check failed with exit code ${exitCode}")
        }
    }

}
