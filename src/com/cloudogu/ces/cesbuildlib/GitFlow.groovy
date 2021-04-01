package com.cloudogu.ces.cesbuildlib

class GitFlow implements Serializable {
    private def script
    private Git git
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
    void finishRelease(String releaseVersion, String productionBranch = "master") {
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
        // Remember latest committer on develop to use as author of release commits
        String releaseBranchAuthor = git.commitAuthorName
        String releaseBranchEmail = git.commitAuthorEmail

        git.checkoutLatest('develop')
        git.checkoutLatest(productionBranch)

        // Merge release branch into productionBranch
        git.mergeNoFastForward(branchName, releaseBranchAuthor, releaseBranchEmail)

        // Create tag. Use -f because the created tag will persist when build has failed.
        git.setTag(releaseVersion, "release version ${releaseVersion}", true)
        // Merge release branch into develop
        git.checkout('develop')
        // Set author of release Branch as author of merge commit
        // Otherwise the author of the last commit on develop would author the commit, which is unexpected
        git.mergeNoFastForward(branchName, releaseBranchAuthor, releaseBranchEmail)

        // Delete release branch
        git.deleteLocalBranch(branchName)

        // Checkout tag
        git.checkout(releaseVersion)

        // Push changes and tags
        git.push("origin ${productionBranch} develop ${releaseVersion}")
        git.deleteOriginBranch(branchName)
    }
}
