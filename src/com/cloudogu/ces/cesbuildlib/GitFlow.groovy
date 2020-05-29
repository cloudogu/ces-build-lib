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
        return git.getBranchName().startsWith('release/')
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

        // Stop the build here if there is already a tag for this version on remote.
        // Do not stop the build when the tag only exists locally
        // because this could mean the build has failed and was restarted.
        if (git.originTagExists("${releaseVersion}")) {
            script.error('You cannot build this version, because it already exists.')
        }

        // Make sure all branches are fetched
        git.fetch()

        // Stop the build if there are new changes on develop that are not merged into this feature branch.
        if (git.originBranchesHaveDiverged(branchName, 'develop')) {
            script.error('There are changes on develop branch that are not merged into release. Please merge and restart process.')
        }

        // Make sure any branch we need exists locally
        git.checkoutLatest(branchName)
        git.checkoutLatest('develop')
        git.checkoutLatest('master')

        // Merge release branch into master
        git.mergeNoFastForward(branchName)

        // Create tag. Use -f because the created tag will persist when build has failed.
        git.setTag(releaseVersion, "release version ${releaseVersion}", true);

        // Merge release branch into develop
        git.checkout('develop')
        git.mergeNoFastForward(branchName)

        // Delete release branch
        git.deleteLocalBranch(branchName)

        // Checkout tag
        git.checkout(releaseVersion)

        // Push changes and tags
        git.push("master develop ${releaseVersion}")
        git.deleteOriginBranch(branchName)
    }
}
