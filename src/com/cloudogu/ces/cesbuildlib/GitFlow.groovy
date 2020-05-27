package com.cloudogu.ces.cesbuildlib

class GitFlow implements Serializable {
    private script
    private git
    Sh sh

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
    void finishRelease(String releaseVersion) {
        String branchName = git.getBranchName()

        // Check if tag already exists
        if (this.git.tagExists("${releaseVersion}")) {
            throw new IllegalStateException("You cannot build this version, because it already exists.")
        }

        // Make sure all branches are fetched
        this.git.fetch()

        // Make sure there are no changes on develop
        if (this.git.branchesHaveDiverged(branchName, "develop")) {
            throw new IllegalStateException("There are changes on develop branch that are not merged into release. Please merge and restart process.")
        }

        // Make sure any branch we need exists locally
        this.git.checkoutAndPull(branchName)
        this.git.checkoutAndPull("develop")
        this.git.checkoutAndPull("master")

        // Merge release branch into master
        this.git.mergeNoFastForward(branchName)

        // Create tag. Use -f because the created tag will persist when build has failed.
        this.git.createTag(releaseVersion, "release version ${releaseVersion}", true);

        // Merge release branch into develop
        this.git.checkout("develop")
        this.git.mergeNoFastForward(branchName)

        // Delete release branch
        this.git.deleteLocalBranch(branchName)

        // Checkout tag
        this.git.checkout(releaseVersion)

        // Push changes and tags
        this.git.push("master")
        this.git.push("develop")
        this.git.push("--tags")
        this.git.deleteOriginBranch(branchName)
    }
}
