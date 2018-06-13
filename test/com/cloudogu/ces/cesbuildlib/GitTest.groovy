package com.cloudogu.ces.cesbuildlib

import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test

import static groovy.util.GroovyTestCase.assertEquals
import static org.junit.Assert.*

class GitTest {

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
    void testCall() throws Exception {
        Git git = new Git(new ScriptMock())
        git.metaClass.git = { String args ->
            return args
        }
        def result = git "https://repoUrl"
        assertEquals("https://repoUrl", result)
    }

    @Test
    void testGetSimpleBranchName() {
        String expectedSimpleBranchName = "simpleName"
        def scriptMock = new ScriptMock()
        scriptMock.env= new Object() {
            String BRANCH_NAME = "feature/somethingelse/$expectedSimpleBranchName"
        }
        Git git = new Git(scriptMock)

        assertEquals(expectedSimpleBranchName, git.simpleBranchName)
    }

    @Test
    void getAuthorEmail() {
        Git git = new Git(createMockedScriptReturnOnSh( "User Name <user.name@doma.in>"))
        assertEquals("user.name@doma.in", git.commitAuthorEmail)
    }

    @Test
    void getAuthorEmailNoMatch() {
        Git git = new Git(createMockedScriptReturnOnSh("does not contain an email"))
        assertEquals("", git.commitAuthorEmail)
    }

    @Test
    void getAuthorName() {
        Git git = new Git(createMockedScriptReturnOnSh("User Name <user.name@doma.in>"))
        assertEquals("User Name", git.commitAuthorName)
    }

    @Test
    void getCommitMessage() {
        String expectedReturnValue = "commit msg"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals(expectedReturnValue, git.commitMessage)
    }

    @Test
    void getCommitHash() {
        String expectedReturnValue = "fb1c8820df462272011bca5fddbe6933e91d69ed"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals(expectedReturnValue, git.commitHash)
    }

    @Test
    void getCommitHashShort() {
        String expectedReturnValue = "1674930"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals(expectedReturnValue, git.commitHashShort)
    }

    @Test
    void getRepositoryUrl() {
        String expectedReturnValue = "https://github.com/orga/repo.git"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals(expectedReturnValue, git.repositoryUrl)
    }

    @Test
    void getGitHubRepositoryName() {
        String expectedReturnValue = "https://github.com/orga/repo"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals('orga/repo', git.gitHubRepositoryName)
    }

    @Test
    void getGitHubRepositoryNameEndingInGit() {
        String expectedReturnValue = "https://github.com/orga/repo.git"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals('orga/repo', git.gitHubRepositoryName)
    }

    @Test
    void getGitHubRepositoryNameNotContainsDotGitSomewhere() {
        String expectedReturnValue = "https://a.git.github.com/orga/repo"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals('orga/repo', git.gitHubRepositoryName)
    }

    @Test
    void getGitHubRepositoryNameNonGitHub() {
        String expectedReturnValue = "https://notGH.info/orga/repo"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals('', git.gitHubRepositoryName)
    }

    @Test
    void getTag() {
        String expectedReturnValue = "our tag"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertEquals(expectedReturnValue, git.tag)
    }

    @Test
    void isTag() {
        String expectedReturnValue = "our tag"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertTrue(git.isTag())
    }

    @Test
    void isNotATag() {
        String expectedReturnValue = "undefined"
        Git git = new Git(createMockedScriptReturnOnSh(expectedReturnValue + " \n"))
        assertFalse(git.isTag())
    }

    @Test
    void commit() {
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValue = "User Name <user.name@doma.in>"
        Git git = new Git(scriptMock)
        git.commit 'msg'
        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == 'User Name'
        assert actualWithEnv['GIT_COMMITTER_NAME'] == 'User Name'
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == 'user.name@doma.in'
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == 'user.name@doma.in'
    }

    @Test
    void createRepoUrlWithCredentials() throws Exception {
        Map<String, Closure> mockedScript = [
                sh: { Map<String, String> args ->
                    return "https://repo.url"
                }
        ]
        Git git = new Git(mockedScript)

        def repoUrlWithCredentials = git.createRepoUrlWithCredentials("u", "pw")
        assertEquals("https://u:pw@repo.url", repoUrlWithCredentials)
    }

    @Test
    void createRepoUrlWithCredentialsUrlWithUserName() throws Exception {
        Map<String, Closure> mockedScript = [
                sh: { Map<String, String> args ->
                    return "https://u@repo.url"
                }
        ]
        Git git = new Git(mockedScript)

        def repoUrlWithCredentials = git.createRepoUrlWithCredentials("u", "pw")
        assertEquals("https://u:pw@repo.url", repoUrlWithCredentials)
    }

    @Test
    void createRepoUrlWithCredentialsUrlWithCredentials() throws Exception {
        Map<String, Closure> mockedScript = [
                sh: { Map<String, String> args ->
                    return "https://u:pw@repo.url"
                }
        ]
        Git git = new Git(mockedScript)

        def repoUrlWithCredentials = git.createRepoUrlWithCredentials("u", "pw")
        assertEquals("https://u:pw@repo.url", repoUrlWithCredentials)
    }

    private static Map<String, Closure> createMockedScriptReturnOnSh(String returnedBySh) {
        return [
                sh: { Map<String, String> args ->
                    return returnedBySh
                }
        ]
    }
}
