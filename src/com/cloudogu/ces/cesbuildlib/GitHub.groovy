package com.cloudogu.ces.cesbuildlib

class GitHub implements Serializable {
    private script
    private git

    GitHub(script, git) {
        this.script = script
        this.git = git
    }

    /**
     * Creates a new release on Github and adds changelog info to it
     *
     * @param releaseVersion the version for the github release
     * @param changelog the changelog object to extract the release information from
     */
    void createGithubRelease(String releaseVersion, Changelog changelog) {
        try {
            def changelogText = changelog.getChangelog(releaseVersion)
            script.echo "The description of github release will be: >>>${changelogText}<<<"
            addGithubRelease(releaseVersion, changelogText)
        } catch (Exception e) {
            script.unstable("Release failed due to error: ${e}")
            script.echo "Please manually update github release."
        }
    }

    /**
     * Creates a release on Github and fills it with the changes provided
     */
    void addGithubRelease(String releaseVersion, String changes) {
        def repositoryName = git.getRepositoryName()
        if (!git.credentials) {
            throw new Exception("Unable to create Github release without credentials")
        }
        script.withCredentials([script.usernamePassword(credentialsId: credentials, usernameVariable: 'GIT_AUTH_USR', passwordVariable: 'GIT_AUTH_PSW')]) {
            def body = "'{\"tag_name\": \"${releaseVersion}\", \"target_commitish\": \"master\", \"name\": \"${releaseVersion}\", \"body\":\"${changes}\"}'"
            def apiUrl = "https://api.github.com/repos/${repositoryName}/releases"
            def flags = "--request POST --data ${body} --header \"Content-Type: application/json\""
            def username = '\$GIT_AUTH_USR'
            def password = '\$GIT_AUTH_PSW'
            script.sh "curl -u ${username}:${password} ${flags} ${apiUrl}"
        }
    }

    /**
     * Commits and pushes a folder to the <code>gh-pages</code> branch of the current repo.
     * Can be used to conveniently deliver websites. See https://pages.github.com/
     *
     * Uses the name and email of the last committer as author and committer.
     *
     * Note that the branch is temporarily checked out to the <code>.gh-pages</code> folder.
     *
     * @param workspaceFolder
     * @param commitMessage
     */
    void pushGitHubPagesBranch(String workspaceFolder, String commitMessage, String subFolder = '.') {
        def ghPagesTempDir = '.gh-pages'
        try {
            script.dir(ghPagesTempDir) {
                this.git.git url: this.git.repositoryUrl, branch: 'gh-pages', changelog: false, poll: false

                script.sh "mkdir -p ${subFolder}"
                script.sh "cp -rf ../${workspaceFolder}/* ${subFolder}"
                this.git.add '.'
                this.git.commit commitMessage
                this.git.push 'gh-pages'
            }
        } finally {
            script.sh "rm -rf ${ghPagesTempDir}"
        }
    }
}
