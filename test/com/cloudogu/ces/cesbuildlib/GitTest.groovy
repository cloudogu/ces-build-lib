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
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        Git git = new Git(scriptMock)
        git.commit 'msg'
        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
    }

    @Test
    void setTag() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        Git git = new Git(scriptMock)
        git.setTag("someTag", "someMessage")
        def actualWithEnv = scriptMock.actualWithEnvAsMap()

        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
        assert scriptMock.actualShStringArgs[0] == "git tag -m \"someMessage\" someTag"
    }

    @Test
    void fetch() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = ""
        Git git = new Git(scriptMock)
        git.fetch()

        assert scriptMock.allActualArgs.size() == 3
        assert scriptMock.allActualArgs[0] == "git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'"
        assert scriptMock.allActualArgs[1] == "git fetch --all > output"
        assert scriptMock.allActualArgs[2] == "cat output"
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
        Git git = new Git(scriptMock)
        git.merge("master")

        assert scriptMock.actualShStringArgs[0] == "git merge master"
    }

    @Test
    void mergeFastForwardOnly() {
        ScriptMock scriptMock = new ScriptMock()
        Git git = new Git(scriptMock)
        git.mergeFastForwardOnly("master")

        assert scriptMock.actualShStringArgs[0] == "git merge --ff-only master"
    }

    @Test
    void push() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master > output', 0)
        scriptMock.expectedShRetValueForScript.put("cat output", "")
        git = new Git(scriptMock, 'creds')
        git.push('master')
    }

    @Test
    void pushNonHttps() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master > output', 0)
        scriptMock.expectedShRetValueForScript.put("cat output", "")
        git = new Git(scriptMock, 'creds')
        git.push('master')

        assert scriptMock.actualShMapArgs.size() == 2
        assert scriptMock.actualShMapArgs.get(0) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master > output'
        assert scriptMock.actualShMapArgs.get(1) == 'cat output'
    }

    @Test
    void pushWithRetry() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master > output', [128, 128, 0])
        scriptMock.expectedShRetValueForScript.put("cat output", "")
        git = new Git(scriptMock, 'creds')
        git.retryTimeout = 1
        git.push('master')

        assert scriptMock.actualShMapArgs.size() == 6
        assert scriptMock.actualShMapArgs.get(0) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master > output'
        assert scriptMock.actualShMapArgs.get(1) == 'cat output'
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master > output'
        assert scriptMock.actualShMapArgs.get(3) == 'cat output'
        assert scriptMock.actualShMapArgs.get(4) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master > output'
        assert scriptMock.actualShMapArgs.get(5) == 'cat output'
    }

    @Test
    void pushNoCredentials() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = ""
        Git git = new Git(scriptMock)
        git.push('master')

        assert scriptMock.actualShMapArgs.size() == 2
        assert scriptMock.actualShMapArgs.get(0) == 'git push origin master > output'
        assert scriptMock.actualShMapArgs.get(1) == 'cat output'
    }

    @Test
    void gitCallWillReturnStdoutAndStatusCode() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat output", "myScriptOutput")
        def expectedGitCall = "git -c credential.helper=\"!f() { echo username='\$GIT_AUTH_USR'; echo password='\$GIT_AUTH_PSW'; }; f\" myGitCommand > output"
        scriptMock.expectedShRetValueForScript.put(expectedGitCall, 0)

        Git git = new Git(scriptMock, "credentials")
        def output = git.executeGitWithCredentials("myGitCommand")

        assertEquals(2, scriptMock.allActualArgs.size())
        assertEquals(expectedGitCall, scriptMock.allActualArgs[0])
        assertEquals("cat output", scriptMock.allActualArgs[1])
        assertEquals("myScriptOutput", output.stdout)
        assertEquals(0, output.exitCode)
    }

    @Test
    void testCreateTag(){
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat output", "myScriptOutput")
        Git git = new Git(scriptMock, "credentials")

        git.createTag("myTag", "myMessage", false)
        git.createTag("myTag", "myMessage", true)

        assertEquals(2, scriptMock.allActualArgs.size())
        assertEquals("git tag -m 'myMessage' myTag", scriptMock.allActualArgs[0])
        assertEquals("git tag -f -m 'myMessage' myTag", scriptMock.allActualArgs[1])
    }
}
