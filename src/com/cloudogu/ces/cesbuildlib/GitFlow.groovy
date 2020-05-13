package com.cloudogu.ces.cesbuildlib

class GitFlow implements Serializable{
    private script
    private git
    Sh sh
    def credentials = null

    GitFlow(script, git, credentials) {
        this(script, git)
        this.credentials = credentials
    }

    GitFlow(script, git) {
        this.script = script
        this.git = git
        this.sh = new Sh(script)
    }

    /**
     * @return if this branch is a release branch according to git flow
     */
    boolean isReleaseBranch() {
        return this.git.getBranchName().startsWith("release/")
    }
}
