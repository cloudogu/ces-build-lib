package com.cloudogu.ces.cesbuildlib

class GitFlow implements Serializable {
    private def script
    private Git git
    Sh sh
    private Makefile makefile

    GitFlow(script, Git git) {
        this.script = script
        this.git = git
        this.sh = new Sh(script)
    }

    GitFlow(script, Git git, Makefile makefile) {
        this(script, git)
        this.makefile = makefile
    }

    /**
     * @return if this branch is a release branch according to git flow
     */
    boolean isReleaseBranch() {
        return git.getBranchName().startsWith('release/')
    }

    /**
     * @return if this branch is the develop branch and therefor ready for pre-release according to git flow
     */
    boolean isPreReleaseBranch() {
        return git.getSimpleBranchName().equals("develop")
    }

    boolean isUnallowedBackportRelease(String productionBranch, String developmentBranch) {
        if (makefile != null) {
            def baseVersion = makefile.getBaseVersion()
            if (baseVersion != null && baseVersion != "" && (!productionBranch.contains(baseVersion) || !developmentBranch.contains(baseVersion)))  {
                return true
            }
        }
        return false
    }

    /**
     * Finishes a git flow release and pushes all merged branches to remote
     *
     * Only execute this function if you are already on a release branch
     *
     * @param releaseVersion the version that is going to be released
     */
    void finishRelease(String releaseVersion, String productionBranch = "master",  String developmentBranch = "develop") {
        String branchName = git.getBranchName()

        // Stop the build here if there is already a tag for this version on remote.
        // Do not stop the build when the tag only exists locally
        // because this could mean the build has failed and was restarted.
        if (git.originTagExists("${releaseVersion}")) {
            script.error('You cannot build this version, because it already exists.')
        }

        // Make sure all branches are fetched
        git.fetch()

        // Check if a backport release is configured by setting BASE_VERSION inside Makefile
        if (isUnallowedBackportRelease(productionBranch, developmentBranch)) {
            script.error('The Variable BASE_VERSION is set in the Makefile. The release should not be merged into main / master / develop or other backport branches.')
        }

        // Stop the build if there are new changes on develop that are not merged into this feature branch.
        if (git.originBranchesHaveDiverged(branchName, developmentBranch)) {
            script.error('There are changes on develop branch that are not merged into release. Please merge and restart process.')
        }

        // Make sure any branch we need exists locally
        git.checkoutLatest(branchName)
        // Remember latest committer on develop to use as author of release commits
        String releaseBranchAuthor = git.commitAuthorName
        String releaseBranchEmail = git.commitAuthorEmail

        git.checkoutLatest(developmentBranch)
        git.checkoutLatest(productionBranch)

        // Merge release branch into productionBranch
        git.mergeNoFastForward(branchName, releaseBranchAuthor, releaseBranchEmail)

        // Create tag. Use -f because the created tag will persist when build has failed.
        git.setTag(releaseVersion, "release version ${releaseVersion}", true)
        // Merge release branch into develop
        git.checkout(developmentBranch)
        // Set author of release Branch as author of merge commit
        // Otherwise the author of the last commit on develop would author the commit, which is unexpected
        git.mergeNoFastForward(branchName, releaseBranchAuthor, releaseBranchEmail)

        // Delete release branch
        git.deleteLocalBranch(branchName)

        // Checkout tag
        git.checkout(releaseVersion)

        // Push changes and tags
        git.push("origin ${productionBranch} ${developmentBranch} ${releaseVersion}")
        git.deleteOriginBranch(branchName)
    }
}
