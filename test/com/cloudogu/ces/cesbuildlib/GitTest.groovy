package com.cloudogu.ces.cesbuildlib

import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test

import static groovy.util.GroovyTestCase.assertEquals
import static org.junit.Assert.*

class GitTest {

    ScriptMock scriptMock = new ScriptMock()
    Git git = new Git(scriptMock)

    def expectedRemoteWithoutCredentials = 'git remote set-url origin https://repo.url'
    def expectedRemoteWithCredentials = 'git remote set-url origin https://username:thePassword@repo.url'

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        Git.metaClass = null
    }

    @Test
    void testClean() throws Exception {
        String expectedParams = "params"
        def scriptMock = new ScriptMock()
        new Git(scriptMock).clean(expectedParams)
        assertThat(scriptMock.actualShStringArgs, Matchers.contains(
                "git clean -df --exclude $expectedParams".toString(),
                'git checkout -- .'))
    }

    @Test
    void testCleanEmpty() throws Exception {
        def scriptMock = new ScriptMock()
        new Git(scriptMock).clean("")
        assertThat(scriptMock.actualShStringArgs, Matchers.contains(
                "git clean -df",
                'git checkout -- .'))
    }

    @Test
    void testCallString() throws Exception {
        Git git = new Git(new ScriptMock())
        String repo = "https://repoUrl"
        def result = git "$repo"
        assertEquals("https://repoUrl", result)
    }

    @Test
    void testCallStringAddsCredentials() throws Exception {
        def creds = "creds"
        Git git = new Git(new ScriptMock(), creds)
        def result = git "https://repoUrl"
        assertEquals("https://repoUrl", result.get('url'))
        assertEquals("creds", result.get('credentialsId'))
    }

    @Test
    void testCallMap() throws Exception {
        Git git = new Git(new ScriptMock())
        def result = git url: "https://repoUrl", credentialsId: "creds"
        assertEquals("https://repoUrl", result.get('url'))
        assertEquals("creds", result.get('credentialsId'))
    }

    @Test
    void testCallMapAddsCredentials() throws Exception {
        def creds = "creds"
        Git git = new Git(new ScriptMock(), creds)
        def result = git url: "https://repoUrl"
        assertEquals("https://repoUrl", result.get('url'))
        assertEquals("creds", result.get('credentialsId'))
    }

    @Test
    void testGetSimpleBranchName() {
        String expectedSimpleBranchName = "simpleName"
        def scriptMock = new ScriptMock()
        scriptMock.env = new Object() {
            String BRANCH_NAME = "feature/somethingelse/$expectedSimpleBranchName"
        }
        Git git = new Git(scriptMock)

        assertEquals(expectedSimpleBranchName, git.simpleBranchName)
    }

    @Test
    void getAuthorEmail() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        assertEquals("user.name@doma.in", git.commitAuthorEmail)
    }

    @Test
    void getAuthorEmailNoMatch() {
        scriptMock.expectedDefaultShRetValue = "does not contain an email"
        assertEquals("", git.commitAuthorEmail)
    }

    @Test
    void getAuthorName() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        assertEquals("User Name", git.commitAuthorName)
    }

    @Test
    void getCommitMessage() {
        String expectedReturnValue = "commit msg"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals(expectedReturnValue, git.commitMessage)
    }

    @Test
    void getCommitHash() {
        String expectedReturnValue = "fb1c8820df462272011bca5fddbe6933e91d69ed"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals(expectedReturnValue, git.commitHash)
    }

    @Test
    void getCommitHashShort() {
        String expectedReturnValue = "1674930"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals(expectedReturnValue, git.commitHashShort)
    }

    @Test
    void getRepositoryUrl() {
        String expectedReturnValue = "https://github.com/orga/repo.git"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals(expectedReturnValue, git.repositoryUrl)
    }

    @Test
    void getRepositoryNameHttps() {
        String expectedReturnValue = "https://bitbucket.org/orga/repo"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.repositoryName)
    }

    @Test
    void getRepositoryNameSsh() {
        String expectedReturnValue = "git@bitbucket.org:orga/repo.git"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.repositoryName)
    }

    @Test
    void getRepositoryNameEndingInGit() {
        String expectedReturnValue = "https://bitbucket.org/orga/repo.git"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.repositoryName)
    }

    @Test
    void getRepositoryNameNotContainsDotGitSomewhere() {
        String expectedReturnValue = "https://a.git.bitbucket.org/orga/repo"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.repositoryName)
    }

    @Test
    void getGitHubRepositoryNameNonGitHub() {
        String expectedReturnValue = "https://bitbucket.org/orga/repo"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('', git.gitHubRepositoryName)
    }

    @Test
    void getTag() {
        String expectedReturnValue = "our tag"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals(expectedReturnValue, git.tag)
    }

    @Test
    void isTag() {
        String expectedReturnValue = "our tag"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertTrue(git.isTag())
    }

    @Test
    void isNotATag() {
        String expectedReturnValue = ""
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertFalse(git.isTag())
    }

    @Test
    void commit() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.commit 'msg'
        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
    }

    @Test
    void setTag() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.setTag("someTag", "someMessage")
        git.setTag("myTag", "myMessage", true)
        def actualWithEnv = scriptMock.actualWithEnvAsMap()

        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
        assert scriptMock.actualShStringArgs[0] == "git tag -m \"someMessage\" someTag"
        assert scriptMock.actualShStringArgs[1] == "git tag -f -m \"myMessage\" myTag"
    }

    @Test
    void fetch() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = ""
        Git git = new Git(scriptMock)
        git.fetch()

        println scriptMock.allActualArgs
        assert scriptMock.allActualArgs[0] == "git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'"
        assert scriptMock.allActualArgs[1] == "git fetch --all"
    }

    @Test
    void "pull with empty refspec"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.pull()

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
        
        assert scriptMock.actualShMapArgs.size() == 3
        assert scriptMock.actualShMapArgs.get(2).trim() == expectedGitCommandWithCredentials
    }

    @Test
    void "pull origin"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull origin'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.pull('origin')

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 3
        assert scriptMock.actualShMapArgs.get(2).trim() == expectedGitCommandWithCredentials
    }

    @Test
    void 'pull master'() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull origin master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.pull 'master'

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 3
        assert scriptMock.actualShMapArgs.get(2).trim() == expectedGitCommandWithCredentials
    }

    @Test
    void 'pull origin master'() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull origin master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.pull 'origin master'

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 3
        assert scriptMock.actualShMapArgs.get(2).trim() == expectedGitCommandWithCredentials
    }

    @Test
    void 'pull upstream master'() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull upstream master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.pull 'upstream master'

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 3
        assert scriptMock.actualShMapArgs.get(2).trim() == expectedGitCommandWithCredentials
    }

    @Test
    void checkout() {
        git.checkout("master")

        assert scriptMock.actualShStringArgs[0] == "git checkout master"
    }

    @Test
    void "checkoutOrCreate() with new branch"() {
        scriptMock.expectedShRetValueForScript.put('git checkout master', 42)
        git.checkoutOrCreate("master")

        assert scriptMock.actualShStringArgs[0] == "git checkout -b master"
    }

    @Test
    void "checkoutOrCreate() with existing branch"() {
        scriptMock.expectedShRetValueForScript.put('git checkout master', 0)
        git.checkoutOrCreate("master")

        assert scriptMock.actualShMapArgs[0] == "git checkout master"
    }

    @Test
    void merge() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        Git git = new Git(scriptMock)
        git.merge("master")
        def actualWithEnv = scriptMock.actualWithEnvAsMap()

        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
        println scriptMock.actualShStringArgs
        assert scriptMock.actualShStringArgs[0] == "git merge master"
    }

    @Test
    void mergeFastForwardOnly() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        Git git = new Git(scriptMock)
        git.mergeFastForwardOnly("master")
        def actualWithEnv = scriptMock.actualWithEnvAsMap()

        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
        assert scriptMock.actualShStringArgs[0] == "git merge --ff-only master"
    }

    @Test
    void mergeNoFastForward() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        Git git = new Git(scriptMock)
        git.mergeNoFastForward("master")
        def actualWithEnv = scriptMock.actualWithEnvAsMap()

        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
        assert scriptMock.actualShStringArgs[0] == "git merge --no-ff master"
    }

    @Test
    void push() {
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master', 0)
        git = new Git(scriptMock, 'creds')
        git.push('master')
    }

    @Test
    void "push with empty refspec"() {
        scriptMock.expectedDefaultShRetValue = 0
        git.push()

        assert scriptMock.actualShMapArgs.size() == 1
        assert scriptMock.actualShMapArgs.get(0).trim() == 'git push'
    }

    @Test
    void "push origin"() {
        scriptMock.expectedDefaultShRetValue = 0
        git.push('origin')

        assert scriptMock.actualShMapArgs.size() == 1
        assert scriptMock.actualShMapArgs.get(0) == 'git push origin'
    }

    @Test
    void "push origin master"() {
        scriptMock.expectedDefaultShRetValue = 0
        git.push('origin master')

        assert scriptMock.actualShMapArgs.size() == 1
        assert scriptMock.actualShMapArgs.get(0) == 'git push origin master'
    }

    @Test
    void "push upstream master"() {
        scriptMock.expectedDefaultShRetValue = 0
        git.push('upstream master')

        assert scriptMock.actualShMapArgs.size() == 1
        assert scriptMock.actualShMapArgs.get(0) == 'git push upstream master'
    }

    @Test
    void pushNonHttps() {
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master', 0)

        git = new Git(scriptMock, 'creds')
        git.push('master')

        assert scriptMock.actualShMapArgs.get(0) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
    }

    @Test
    void pushWithRetry() {
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master', [128, 128, 0])
        git = new Git(scriptMock, 'creds')
        git.retryTimeout = 1
        git.push('master')

        assert scriptMock.actualShMapArgs.get(0) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        assert scriptMock.actualShMapArgs.get(1) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
    }

    @Test
    void "pushAndPullOnFailure with empty refspec"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, [1, 0])
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure()

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2).trim() == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push'
        assert scriptMock.actualShMapArgs.get(3).trim() == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull'
        assert scriptMock.actualShMapArgs.get(4).trim() == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push'
    }

    @Test
    void "pushAndPullOnFailure master"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, [1, 0])
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull origin master', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure('master')

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        assert scriptMock.actualShMapArgs.get(3) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull origin master'
        assert scriptMock.actualShMapArgs.get(4) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
    }

    @Test
    void "pushAndPullOnFailure origin master"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, [1, 0])
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull origin master', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure('origin master')

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        assert scriptMock.actualShMapArgs.get(3) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull origin master'
        assert scriptMock.actualShMapArgs.get(4) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
    }

    @Test
    void "pushAndPullOnFailure upstream master"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push upstream master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, [1, 0])
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull upstream master', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure('upstream master')

        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'

        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push upstream master'
        assert scriptMock.actualShMapArgs.get(3) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull upstream master'
        assert scriptMock.actualShMapArgs.get(4) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push upstream master'
    }

    @Test
    void pushNoCredentials() {
        scriptMock.expectedDefaultShRetValue = 0
        git.retryTimeout = 1
        git.push('master')

        assert scriptMock.actualShMapArgs.get(0) == 'git push origin master'
    }
}
