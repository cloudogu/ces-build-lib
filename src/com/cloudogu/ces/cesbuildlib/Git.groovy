package com.cloudogu.ces.cesbuildlib

class Git implements Serializable {
    def script
    Sh sh
    def credentials = null

    Git(script, credentials) {
        this(script)
        this.credentials = credentials
    }

    Git(script) {
        this.script = script
        this.sh = new Sh(script)
    }

    def call(args) {
        git(args)
    }

    /**
     * Credential-aware wrapper around the global "git" step.
     */
    private def git(args) {

        if (credentials != null) {

            // args instanceof Map ist not allowed due to sandboxing. So find it out the hard way :-(
            try {
                if (!args.containsKey('credentialsId')) {
                    args.put('credentialsId', credentials)
                }
            } catch (MissingMethodException ignored) {
                // This exception indicates that we don't have a Map. Assume url String and add credentials
                args = [url: args.toString(), credentialsId: credentials]
            }
        }
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
     * @return the part of the branch name after the slash
     */
    String getSimpleBranchName() {
        return branchName.substring(branchName.lastIndexOf('/') + 1)
    }

    /**
     * @return the Git Author of HEAD, in the following form <code>User Name &lt;user.name@doma.in&gt;</code>
     */
    String getCommitAuthorComplete() {
        sh.returnStdOut "git --no-pager show -s --format='%an <%ae>' HEAD"
    }

    String getCommitAuthorName() {
        return getCommitAuthorComplete().replaceAll(" <.*", "")
    }

    String getCommitAuthorEmail() {
        def matcher = getCommitAuthorComplete() =~ "<(.*?)>"
        matcher ? matcher[0][1] : ""
    }

    String getCommitMessage() {
        sh.returnStdOut "git log -1 --pretty=%B"
    }

    String getCommitHash() {
        sh.returnStdOut "git rev-parse HEAD"
    }

    String getCommitHashShort() {
        sh.returnStdOut "git rev-parse --short HEAD"
    }

    /**
     * @return the URL of the Git repository, e.g. {@code https://github.com/orga/repo.git}
     */
    String getRepositoryUrl() {
        sh.returnStdOut "git config --get remote.origin.url"
    }

    /**
     * @return the name of the GitHub Repository e.g. {@code repository/url} or empty String, if no GitHub repo.
     */
    String getGitHubRepositoryName() {
        String repoUrl = repositoryUrl
        if (!repoUrl.contains('github.com')) {
            return ''
        }
        def potentialRepoName = repoUrl.substring(repositoryUrl.indexOf('github.com') + 'github.com'.length() + 1)
        if (potentialRepoName.endsWith('.git')) {
            return potentialRepoName.substring(0, potentialRepoName.length() - 4)
        }
        return potentialRepoName
    }

    String getTag() {
        return sh.returnStdOut("git name-rev --name-only --tags HEAD")
    }

    boolean isTag() {
        return getTag() != "undefined"
    }

    def add(String pathspec) {
        script.sh "git add $pathspec"
    }

    /**
     * Commits using the name and email of the last committer as author and committer.
     *
     * @param message
     */
    void commit(String message) {
        commit(message, commitAuthorName, commitAuthorEmail)
    }

    /**
     * Commits using the specific name and emails as author and committer.
     *
     * @param message
     */
    void commit(String message, String authorName, String authorEmail) {
        script.withEnv(["GIT_AUTHOR_NAME=$authorName", "GIT_AUTHOR_EMAIL=$authorEmail",
                        "GIT_COMMITTER_NAME=$authorName", "GIT_COMMITTER_EMAIL=$authorEmail"]) {
            script.sh "git commit -m \"$message\""
        }
    }

    /**
     * Pushes local to remote repo.
     *
     * @param refSpec branch or tag name
     */
    void push(String refSpec) {
        executeInShellWithGitCredentialsForWriting "git push origin ${refSpec}"
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
    void pushGitHubPagesBranch(String workspaceFolder, String commitMessage) {
        def ghPagesTempDir = '.gh-pages'
        try {
            script.dir(ghPagesTempDir) {
                git url: repositoryUrl, branch: 'gh-pages', changelog: false, poll: false

                script.sh "cp -rf ../${workspaceFolder}/* ."
                add '.'
                commit commitMessage
                push 'gh-pages'
            }
        } finally {
            script.sh "rm -rf ${ghPagesTempDir}"
        }
    }

    /**
     * There seems to be no secure way of pushing to git which credentials, we have to write them to the URL
     * See also
     * https://github.com/jenkinsci/pipeline-examples/blob/0b834c0691b96d8dfc49229ba6effd66470bdee4/pipeline-examples/push-git-repo/pushGitRepo.groovy
     * Our workaround: Explicitly replace any credentials in stdout and stderr before output
     *
     * @param shCommand
     */
    protected void executeInShellWithGitCredentialsForWriting(String shCommand) {

        /* Writing credentials into the remote  will only work for https, not for ssh.
         * However, ssh seems to work without further auth, wen using the git step in pipelines, so just try
         * without setting credentials for ssh remotes.
         * See https://stackoverflow.com/a/38784011/1845976 */
        String repoUrlWithoutCredentials = repositoryUrl

        if (credentials == null || !repoUrlWithoutCredentials.startsWith('https://')) {
            script.sh shCommand
        } else {

            // Avoid credentials being written to stdout and stderr (sh.returnStdOut() will only capture stdout!)
            // Write all output to unique file for this job
            String stdOutAndErrFile = "/tmp/${script.env.BUILD_TAG}-shellout"

            try {
                writeCredentialsIntoGitRemote(repoUrlWithoutCredentials)

                script.sh "${shCommand} > ${stdOutAndErrFile} 2>&1"

            } finally {

                setRemote(repoUrlWithoutCredentials)

                // Remove credentials from stdout, then echo
                script.withCredentials([script.usernamePassword(credentialsId: credentials,
                        passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {

                    String output = sh.returnStdOut("cat ${stdOutAndErrFile}")
                    script.echo(output.replace(script.env.USERNAME, '****').replace(script.env.PASSWORD, '****'))
                    script.sh "rm -f ${stdOutAndErrFile}"
                }
            }
        }
    }


    protected void writeCredentialsIntoGitRemote(String repoUrl) {
        script.withCredentials([script.usernamePassword(credentialsId: credentials,
                passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            def repoUrlWithCredentials = createRepoUrlWithCredentials(repoUrl, script.env.USERNAME, script.env.PASSWORD)
            // Set username and PW, so subsequent operations (fetch, pull, etc.) on remote will succeed
            setRemote(repoUrlWithCredentials)
        }
    }

    /**
     * @return a string that contains the repo url like this. <code>https://${username}:${password}@${plainRepoUrl}</code>
     */
    protected String createRepoUrlWithCredentials(String repoUrl, username, password) {
        def urlPrefixWithUserNameAndPassword = "https://$username:$password@"

        if (repoUrl.startsWith(urlPrefixWithUserNameAndPassword)) {
            return repoUrl
        }
        if (repoUrl.startsWith("https://$username")) {
            return repoUrl.replaceAll("https://$username@", urlPrefixWithUserNameAndPassword)
        }
        return repoUrl.replaceAll("https://", urlPrefixWithUserNameAndPassword)
    }

    protected setRemote(String remote) {
        script.sh "git remote set-url origin ${remote}"
    }
}