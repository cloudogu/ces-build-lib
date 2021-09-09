package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonSlurper

class GitHub implements Serializable {
    private script
    private git
    Sh sh

    GitHub(script, git) {
        this.script = script
        this.git = git
        this.sh = new Sh(script)
    }

    void addReleaseAsset(String releaseId, String filePath) {
        def repositoryName = git.getRepositoryName()

        try {
            script.withCredentials([script.usernamePassword(
                credentialsId: git.credentials, usernameVariable: 'GIT_AUTH_USR', passwordVariable: 'GIT_AUTH_PSW')]) {

                def apiUrl = "https://uploads.github.com/repos/${repositoryName}/releases/${releaseId}/assets?name=\$(basename ${filePath})"
                def flags = """--header "Content-Type: multipart/form-data" --data-binary @${filePath}"""
                def username = '\$GIT_AUTH_USR'
                def password = '\$GIT_AUTH_PSW'
                this.sh.returnStdOut "curl -u ${username}:${password} ${flags} ${apiUrl}"
            }
        } catch (Exception e) {
            script.unstable("Asset upload failed due to error: ${e}")
            script.echo 'Please manually upload asset.'
        }
    }

    /**
     * Creates a new release on Github and adds changelog info to it
     *
     * @param releaseVersion the version for the github release
     * @param changelog the changelog object to extract the release information from
     */
    String createReleaseWithChangelog(String releaseVersion, Changelog changelog, String productionBranch = "master") {
        try {
            def changelogText = changelog.changesForVersion(releaseVersion)
            script.echo "The description of github release will be: >>>${changelogText}<<<"
            createRelease(releaseVersion, changelogText, productionBranch)
        } catch (IllegalArgumentException e) {
            script.unstable("Release failed due to error: ${e}")
            script.echo 'Please manually update github release.'
        }
    }

    /**
     * Creates a release on Github and fills it with the changes provided
     */
    String createRelease(String releaseVersion, String changes, String productionBranch = "master") {
        def repositoryName = git.getRepositoryName()
        if (!git.credentials) {
            throw new IllegalArgumentException('Unable to create Github release without credentials.')
        }
        script.withCredentials([script.usernamePassword(
            credentialsId: git.credentials, usernameVariable: 'GIT_AUTH_USR', passwordVariable: 'GIT_AUTH_PSW')]) {

            def body =
                """{"tag_name": "${releaseVersion}", "target_commitish": "${productionBranch}", "name": "${releaseVersion}", "body":"${changes}"}"""
            def apiUrl = "https://api.github.com/repos/${repositoryName}/releases"
            def flags = """--request POST --data '${body.trim()}' --header "Content-Type: application/json" """
            def username = '\$GIT_AUTH_USR'
            def password = '\$GIT_AUTH_PSW'
            def var=this.sh.returnStdOut("curl -u ${username}:${password} ${flags} ${apiUrl}")
            def jsonSlurper = new JsonSlurper()
            return jsonSlurper.parseText(var).id
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
    void pushPagesBranch(String workspaceFolder, String commitMessage, String subFolder = '.') {
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
