package com.cloudogu.ces.cesbuildlib

import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import static groovy.util.GroovyTestCase.assertEquals
import static org.junit.jupiter.api.Assertions.*
import static org.hamcrest.MatcherAssert.assertThat;

class GitTest {

    ScriptMock scriptMock = new ScriptMock()
    Git git = new Git(scriptMock)

    static final EXPECTED_COMMITTER_NAME = 'U 2'
    static final EXPECTED_COMMITTER_EMAIL = 'user-numb@t.wo'

    @AfterEach
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
    void changesStagedForCommit() {
        scriptMock.expectedDefaultShRetValue = 1
        assertTrue git.areChangesStagedForCommit()
    }

    @Test
    void noChangesStagedForCommit() {
        scriptMock.expectedDefaultShRetValue = 0
        assertFalse git.areChangesStagedForCommit()
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
        assertAuthor('User Name', 'user.name@doma.in')
    }
    
    @Test
    void 'commit with different committer'() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.committerName = EXPECTED_COMMITTER_NAME
        git.committerEmail = EXPECTED_COMMITTER_EMAIL
        git.commit 'msg'
        assertAuthor('User Name', 'user.name@doma.in',
                EXPECTED_COMMITTER_NAME, EXPECTED_COMMITTER_EMAIL)
    }

    @Test
    void setTag() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.setTag("someTag", "someMessage")
        git.setTag("myTag", "myMessage", true)
        
        assertAuthor('User Name', 'user.name@doma.in')
        
        assert scriptMock.actualShStringArgs[0] == "git tag -m \"someMessage\" someTag"
        assert scriptMock.actualShStringArgs[1] == "git tag -f -m \"myMessage\" myTag"
    }
    
    @Test
    void 'setTag with different committer'() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.committerName = EXPECTED_COMMITTER_NAME
        git.committerEmail = EXPECTED_COMMITTER_EMAIL
        
        git.setTag("someTag", "someMessage")

        assertAuthor('User Name', 'user.name@doma.in',
                EXPECTED_COMMITTER_NAME, EXPECTED_COMMITTER_EMAIL)
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
    void pull() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.pull()

        assertAuthor('User Name', 'user.name@doma.in')
        
        assert scriptMock.actualShMapArgs.size() == 3
        assert scriptMock.actualShMapArgs.get(2).trim() == expectedGitCommandWithCredentials
    }
    
    @Test
    void 'pull with different committer'() {
        git.committerName = EXPECTED_COMMITTER_NAME
        git.committerEmail = EXPECTED_COMMITTER_EMAIL

        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')

        git.pull()

        assertAuthor('User Name', 'user.name@doma.in',
                EXPECTED_COMMITTER_NAME, EXPECTED_COMMITTER_EMAIL)
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
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.merge("master")

        assertAuthor('User Name', 'user.name@doma.in')
        assert scriptMock.actualShStringArgs[0] == "git merge master"
    }
    
    @Test
    void 'merge with different committer'() {
        git.committerName = EXPECTED_COMMITTER_NAME
        git.committerEmail = EXPECTED_COMMITTER_EMAIL

        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.merge("master")

        assertAuthor('User Name', 'user.name@doma.in',
                EXPECTED_COMMITTER_NAME, EXPECTED_COMMITTER_EMAIL)
        assert scriptMock.actualShStringArgs[0] == "git merge master"
    }

    @Test
    void mergeFastForwardOnly() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.mergeFastForwardOnly("master")

        assertAuthor('User Name', 'user.name@doma.in')
        assert scriptMock.actualShStringArgs[0] == "git merge --ff-only master"
    }

    @Test
    void mergeNoFastForward() {
        scriptMock.expectedDefaultShRetValue = "User Name <user.name@doma.in>"
        git.mergeNoFastForward("master")

        assertAuthor('User Name', 'user.name@doma.in')
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
        // This is somewhat unexpected and to be resolved with #44
        assert scriptMock.actualShMapArgs.get(0).trim() == 'git push origin'
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
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure()

        assertAuthor('User Name', 'user.name@doma.in')

        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2).trim() == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push'
        assert scriptMock.actualShMapArgs.get(3).trim() == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase'
        assert scriptMock.actualShMapArgs.get(4).trim() == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push'
    }

    @Test
    void "pushAndPullOnFailure master"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, [1, 0])
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase origin master', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure('origin master')

        assertAuthor('User Name', 'user.name@doma.in')
        
        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        assert scriptMock.actualShMapArgs.get(3) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase origin master'
        assert scriptMock.actualShMapArgs.get(4) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
    }

    @Test
    void "pushAndPullOnFailure origin master"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, [1, 0])
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase origin master', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure('origin master')

        assertAuthor('User Name', 'user.name@doma.in')

        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
        assert scriptMock.actualShMapArgs.get(3) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase origin master'
        assert scriptMock.actualShMapArgs.get(4) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push origin master'
    }

    @Test
    void "pushAndPullOnFailure upstream master"() {
        def expectedGitCommandWithCredentials = 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push upstream master'
        scriptMock.expectedShRetValueForScript.put(expectedGitCommandWithCredentials, [1, 0])
        scriptMock.expectedShRetValueForScript.put('git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase upstream master', 0)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD', 'User Name <user.name@doma.in>')
        git = new Git(scriptMock, 'creds')

        git.retryTimeout = 1
        git.pushAndPullOnFailure('upstream master')

        assertAuthor('User Name', 'user.name@doma.in')

        assert scriptMock.actualShMapArgs.size() == 5
        assert scriptMock.actualShMapArgs.get(2) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push upstream master'
        assert scriptMock.actualShMapArgs.get(3) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" pull --rebase upstream master'
        assert scriptMock.actualShMapArgs.get(4) == 'git -c credential.helper="!f() { echo username=\'$GIT_AUTH_USR\'; echo password=\'$GIT_AUTH_PSW\'; }; f" push upstream master'
    }

    @Test
    void pushNoCredentials() {
        scriptMock.expectedDefaultShRetValue = 0
        git.retryTimeout = 1
        git.push('master')

        assert scriptMock.actualShMapArgs.get(0) == 'git push origin master'
    }

    private void assertAuthor(String authorName, String authorEmail,
                              String committerName = authorName, String committerEmail = authorEmail) {
        def actualWithEnv = scriptMock.actualWithEnvAsMap()
        assert actualWithEnv['GIT_AUTHOR_NAME'] == authorName
        assert actualWithEnv['GIT_COMMITTER_NAME'] == committerName
        assert actualWithEnv['GIT_AUTHOR_EMAIL'] == authorEmail
        assert actualWithEnv['GIT_COMMITTER_EMAIL'] == committerEmail
    }
}
