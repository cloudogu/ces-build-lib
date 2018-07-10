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
        scriptMock.env= new Object() {
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
    void getGitHubRepositoryNameHttps() {
        String expectedReturnValue = "https://github.com/orga/repo"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.gitHubRepositoryName)
    }

    @Test
    void getGitHubRepositoryNameSsh() {
        String expectedReturnValue = "git@github.com:orga/repo.git"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.gitHubRepositoryName)
    }

    @Test
    void getGitHubRepositoryNameEndingInGit() {
        String expectedReturnValue = "https://github.com/orga/repo.git"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.gitHubRepositoryName)
    }

    @Test
    void getGitHubRepositoryNameNotContainsDotGitSomewhere() {
        String expectedReturnValue = "https://a.git.github.com/orga/repo"
        scriptMock.expectedDefaultShRetValue = expectedReturnValue + " \n"
        assertEquals('orga/repo', git.gitHubRepositoryName)
    }

    @Test
    void getGitHubRepositoryNameNonGitHub() {
        String expectedReturnValue = "https://notGH.info/orga/repo"
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
        String expectedReturnValue = "undefined"
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
    void createRepoUrlWithCredentials() throws Exception {
        Git git = new Git(new ScriptMock())

        def repoUrlWithCredentials = git.createRepoUrlWithCredentials("https://repo.url", "u", "pw")
        assertEquals("https://u:pw@repo.url", repoUrlWithCredentials)
    }

    @Test
    void createRepoUrlWithCredentialsUrlWithUserName() throws Exception {
        Git git = new Git(new ScriptMock())

        def repoUrlWithCredentials = git.createRepoUrlWithCredentials("https://u@repo.url", "u", "pw")
        assertEquals("https://u:pw@repo.url", repoUrlWithCredentials)
    }

    @Test
    void createRepoUrlWithCredentialsUrlWithCredentials() throws Exception {
        Git git = new Git(new ScriptMock())
        def repoUrlWithCredentials = git.createRepoUrlWithCredentials("https://u:pw@repo.url", "u", "pw")
        assertEquals("https://u:pw@repo.url", repoUrlWithCredentials)
    }

    @Test
    void push() {
        prepareGitPush()
        git = new Git(scriptMock, 'creds')

        git.push('master')

        assert scriptMock.actualEcho.get(0) == 'Contains\nOur ****\n And ****'
        assertRemoteRestored()
    }

    @Test
    void pushRestoreRemoteInCaseOfError() {
        scriptMock= new ScriptMock() {
            String sh(String params) {
                String ret = super.sh(params)
                if (params.contains('push')) {
                    throw new RuntimeException("MockedException")
                }
                ret
            }
        }
        git = new Git(scriptMock, 'creds')
        prepareGitPush()

        try {
            git.push('master')
            fail("Expected exception")
        } catch (RuntimeException e) {
            assertRemoteRestored()
        }
    }

    @Test
    void pushNonHttps() {
        prepareGitPush()
        scriptMock.expectedShRetValueForScript.put('git config --get remote.origin.url', "git@github.com:cloudogu/ces-build-lib.git")

        git.push('master')

        assert scriptMock.actualShStringArgs.size() == 1
        assert scriptMock.actualShStringArgs.get(0) == 'git push origin master'
    }

    @Test
    void pushNoCredentials() {
        prepareGitPush()
        git.push('master')

        assert scriptMock.actualShStringArgs.size() == 1
        assert scriptMock.actualShStringArgs.get(0) == 'git push origin master'
    }

    @Test
    void pushGitHubPagesBranch() {
        prepareGitPush()
        scriptMock.expectedShRetValueForScript.put("git --no-pager show -s --format='%an <%ae>' HEAD", "User Name <user.name@doma.in>")

        git.pushGitHubPagesBranch('website', 'Deploys new version of website')

        assert scriptMock.actualGitArgs.url == "https://repo.url"
        assert scriptMock.actualGitArgs.branch == "gh-pages"

        assert scriptMock.actualDir == '.gh-pages'
        assert scriptMock.actualShStringArgs.contains('cp -rf ../website/* .')
        assert scriptMock.actualShStringArgs.contains('git add .')
        assert scriptMock.actualShStringArgs.contains('git commit -m "Deploys new version of website"')
        assert scriptMock.actualWithEnv.contains("${'GIT_AUTHOR_NAME=User Name'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_COMMITTER_NAME=User Name'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_AUTHOR_EMAIL=user.name@doma.in'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_COMMITTER_EMAIL=user.name@doma.in'}")
        assert scriptMock.actualShStringArgs.contains('git push origin gh-pages')
        assert scriptMock.actualShStringArgs.last == 'rm -rf .gh-pages'
    }

    private void prepareGitPush() {
        scriptMock.env = new Object() {
            String BUILD_TAG = "ourBuildTag"
            String USERNAME = "username"
            String PASSWORD = "thePassword"
        }
        scriptMock.expectedShRetValueForScript.put('git config --get remote.origin.url', "https://repo.url")
        def shellOutputWithCredentials = "Contains\nOur username\n And thePassword"
        scriptMock.expectedShRetValueForScript.put('cat /tmp/ourBuildTag-shellout', shellOutputWithCredentials)
    }

    private assertRemoteRestored() {
        assert scriptMock.actualShStringArgs.contains('git push origin master > /tmp/ourBuildTag-shellout 2>&1')
        assert scriptMock.actualShStringArgs.contains(expectedRemoteWithCredentials)
        assert scriptMock.actualShStringArgs.findIndexOf { it == expectedRemoteWithoutCredentials } >
                scriptMock.actualShStringArgs.findIndexOf { it == expectedRemoteWithCredentials }
        assert scriptMock.actualShStringArgs.last == 'rm -f /tmp/ourBuildTag-shellout'
    }
}
