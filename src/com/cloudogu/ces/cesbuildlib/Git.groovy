package com.cloudogu.ces.cesbuildlib

class Git implements Serializable {
    def script

    Git(script) {
        this.script = script
    }

    def call(args) {
        script.git args
    }

    void clean(String excludes) {
        String excludesParam = ""
        if (excludes != null && "" != excludes) {
            excludesParam = " --exclude $excludes"
        }
        // Remove all untracked files
        script.sh "git clean -df$excludesParam"
        //Clear all unstaged changes
        script.sh 'git checkout -- .'
    }

    /**
     * This is just a convenience wrapper around {@code env.BRANCH_NAME}, provided by jenkins.
     * <b>Only works for multi-branch projects!</b>
     *
     * @return name of the current branch.
     */
    String getBranchName() {
        script.env.BRANCH_NAME
    }

    /**
     * @return the part of the branch name after the last slash
     */
    String getSimpleBranchName() {
        return branchName.substring(branchName.lastIndexOf('/') + 1)
    }

    /**
     * @return the Git Author of HEAD, in the following form <code>User Name &lt;user.name@doma.in&gt;</code>
     */
    String getLastCommitAuthorComplete() {
        return script.sh (
                script: "git --no-pager show -s --format='%an <%ae>' HEAD", returnStdout: true)
    }

    String getLastCommitAuthorName() {
        return getLastCommitAuthorComplete().replaceAll(" <.*", "")
    }

    String getLastCommitAuthorEmail() {
        def matcher = getLastCommitAuthorComplete() =~ "<(.*?)>"
        matcher ? matcher[0][1] : ""
    }

    String getLastCommitMessage() {
        return script.sh (
                script: "git log -1 --pretty=%B", returnStdout: true)
    }

    String getLastCommitHash() {
        return script.sh (
                script: "git rev-parse HEAD", returnStdout: true)
    }

    String getLastCommitHashShort() {
        return script.sh (
                script: "git rev-parse --short HEAD", returnStdout: true)
    }
}