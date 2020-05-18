package com.cloudogu.ces.cesbuildlib

class GitFlow implements Serializable {
    private script
    private git
    Sh sh
    def credentials = null

    GitFlow(script, Git git, credentials) {
        this(script, git)
        this.credentials = credentials
    }

    GitFlow(script, Git git) {
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

    /**
     * Finishes a git flow release and pushes all merged branches to remote
     *
     * Only execute this function if you are already on a release branch
     *
     * @param releaseVersion the version that is going to be released
     */
    void finishGitRelease(String releaseVersion) {
        String branchName = getBranchName()

        // Check if tag already exists
        if (this.git.tagExists("${releaseVersion}")) {
            error("You cannot build this version, because it already exists.")
        }

        // Make sure all branches are fetched
        script.sh "git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'"
        this.git.executeGitWithCredentials("fetch --all")

        // Make sure there are no changes on develop
        if (this.git.developHasChanged(branchName)) {
            error("There are changes on develop branch that are not merged into release. Please merge and restart process.")
        }

        // Make sure any branch we need exists locally
        script.sh "git checkout ${branchName}"
        this.gitexecuteGitWithCredentials("pull origin ${branchName}")
        script.sh "git checkout develop"
        this.git.executeGitWithCredentials("pull origin develop")
        script.sh "git checkout master"
        this.git.executeGitWithCredentials("pull origin master")

        // Merge release branch into master
        script.sh "git merge --no-ff ${branchName}"

        // Create tag. Use -f because the created tag will persist when build has failed.
        this.git.executeGitWithCredentials("tag -f -m 'release version ${releaseVersion}' ${releaseVersion}")

        // Merge release branch into develop
        script.sh "git checkout develop"
        script.sh "git merge --no-ff ${branchName}"

        // Delete release branch
        script.sh "git branch -d ${branchName}"

        // Checkout tag
        script.sh "git checkout ${releaseVersion}"

        // Push changes and tags
        this.git.executeGitWithCredentials("push origin master")
        this.git.executeGitWithCredentials("push origin develop")
        this.git.executeGitWithCredentials("push origin --tags")
        this.git.executeGitWithCredentials("push origin --delete ${branchName}")
    }
}
