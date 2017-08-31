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
     * @return the part of the branch name after the  slash
     */
    String getSimpleBranchName() {
        return branchName.substring(branchName.lastIndexOf('/') + 1)
    }

    /**
     * @return the Git Author of HEAD, in the following form <code>User Name &lt;user.name@doma.in&gt;</code>
     */
    String getCommitAuthorComplete() {
        return script.sh (
                script: "git --no-pager show -s --format='%an <%ae>' HEAD", returnStdout: true)
    }

    String getCommitAuthorName() {
        return getCommitAuthorComplete().replaceAll(" <.*", "")
    }

    String getCommitAuthorEmail() {
        def matcher = getCommitAuthorComplete() =~ "<(.*?)>"
        matcher ? matcher[0][1] : ""
    }

    String getCommitMessage() {
        return shReturnStdout("git log -1 --pretty=%B")
    }

    String getCommitHash() {
        return shReturnStdout("git rev-parse HEAD")
    }

    String getCommitHashShort() {
        return shReturnStdout("git rev-parse --short HEAD")
    }

    String shReturnStdout(String shScript) {
        // Trim to remove trailing line breaks, which result in unwanted behavior in Jenkinsfiles:
        // E.g. when using output in other sh() calls leading to executing the sh command after the line breaks,
        // possibly discarding additional arguments
        return script.sh (script: shScript, returnStdout: true).trim()
    }
}