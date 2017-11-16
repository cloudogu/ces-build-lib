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
        assertThat(scriptMock.shArgs, Matchers.contains(
                "git clean -df --exclude $expectedParams".toString(),
                'git checkout -- .'))
    }

    @Test
    void testCleanEmpty() throws Exception {
        def scriptMock = new ScriptMock()
        new Git(scriptMock).clean("")
        assertThat(scriptMock.shArgs, Matchers.contains(
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

    private static Map<String, Closure> createMockedScriptReturnOnSh(String returnedBySh) {
        return [
                sh: { Map<String, String> args ->
                    return returnedBySh
                }
        ]
    }

    private static class ScriptMock {
        List<String> shArgs = new LinkedList<>()
        def env

        void sh(String args) {
            shArgs.add(args)
        }

        def git(def args) {
            return args
        }
    }
}
