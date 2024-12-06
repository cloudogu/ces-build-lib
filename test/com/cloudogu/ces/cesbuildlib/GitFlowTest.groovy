package com.cloudogu.ces.cesbuildlib

import org.junit.Test
import static org.junit.Assert.*
import static groovy.test.GroovyAssert.shouldFail

class GitFlowTest {
    def scriptMock = new ScriptMock()
    
    @Test
    void testIsReleaseBranch() {
        String branchPrefixRelease = "release"
        String branchPrefixFeature = "feature"

        def scriptMock1 = new ScriptMock()
        scriptMock1.env = new Object() {
            String BRANCH_NAME = "$branchPrefixRelease/something"
        }
        Git git1 = new Git(scriptMock1)
        GitFlow gitflow1 = new GitFlow(scriptMock1, git1)

        def scriptMock2 = new ScriptMock()
        scriptMock2.env = new Object() {
            String BRANCH_NAME = "$branchPrefixFeature/something"
        }
        Git git2 = new Git(scriptMock2)
        GitFlow gitflow2 = new GitFlow(scriptMock2, git2)

        assertTrue(gitflow1.isReleaseBranch())
        assertFalse(gitflow2.isReleaseBranch())
    }

    @Test
    void testFinishRelease() {
        String releaseBranchAuthorName = 'release'
        String releaseBranchEmail = 'rele@s.e'
        String releaseBranchAuthor = createGitAuthorString(releaseBranchAuthorName, releaseBranchEmail)
        String developBranchAuthorName = 'develop'
        String developBranchEmail = 'develop@a.a'
        String developBranchAuthor = createGitAuthorString(developBranchAuthorName, developBranchEmail)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD',
            [releaseBranchAuthor, releaseBranchAuthor, releaseBranchAuthor, releaseBranchAuthor,
             // these two are the ones where the release branch author is stored:
             releaseBranchAuthor, releaseBranchAuthor,
             developBranchAuthor, developBranchAuthor
            ])
        scriptMock.expectedShRetValueForScript.put('git push origin master develop myVersion', 0)

        scriptMock.expectedDefaultShRetValue = ""
        scriptMock.env.BRANCH_NAME = "myReleaseBranch"
        Git git = new Git(scriptMock)
        GitFlow gitflow = new GitFlow(scriptMock, git)
        gitflow.finishRelease("myVersion")

        scriptMock.allActualArgs.removeAll("echo ")
        scriptMock.allActualArgs.removeAll("git --no-pager show -s --format='%an <%ae>' HEAD")
        int i = 0
        assertEquals("git ls-remote origin refs/tags/myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'", scriptMock.allActualArgs[i++])
        assertEquals("git fetch --all", scriptMock.allActualArgs[i++])
        assertEquals("git log origin/myReleaseBranch..origin/develop --oneline", scriptMock.allActualArgs[i++])
        assertEquals("git checkout myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git reset --hard origin/myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git checkout develop", scriptMock.allActualArgs[i++])
        assertEquals("git reset --hard origin/develop", scriptMock.allActualArgs[i++])
        assertEquals("git checkout master", scriptMock.allActualArgs[i++])
        assertEquals("git reset --hard origin/master", scriptMock.allActualArgs[i++])

        // Author & Email 1 (calls 'git --no-pager...' twice)
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[i++])
        assertAuthor(0, releaseBranchAuthorName, releaseBranchEmail)

        // Author & Email 2 (calls 'git --no-pager...' twice)
        assertEquals("git tag -f -m \"release version myVersion\" myVersion", scriptMock.allActualArgs[i++])
        assertAuthor(1, releaseBranchAuthorName, releaseBranchEmail)

        assertEquals("git checkout develop", scriptMock.allActualArgs[i++])
        // Author & Email 3 (calls 'git --no-pager...' twice)
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[i++])
        assertAuthor(2, releaseBranchAuthorName, releaseBranchEmail)

        assertEquals("git branch -d myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git checkout myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git push origin master develop myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git push --delete origin myReleaseBranch", scriptMock.allActualArgs[i++])
    }

    @Test
    void testFinishReleaseWithMainBranch() {
        String releaseBranchAuthorName = 'release'
        String releaseBranchEmail = 'rele@s.e'
        String releaseBranchAuthor = createGitAuthorString(releaseBranchAuthorName, releaseBranchEmail)
        String developBranchAuthorName = 'develop'
        String developBranchEmail = 'develop@a.a'
        String developBranchAuthor = createGitAuthorString(developBranchAuthorName, developBranchEmail)
        scriptMock.expectedShRetValueForScript.put('git --no-pager show -s --format=\'%an <%ae>\' HEAD',
            [releaseBranchAuthor, releaseBranchAuthor, releaseBranchAuthor, releaseBranchAuthor,
             // these two are the ones where the release branch author is stored:
             releaseBranchAuthor, releaseBranchAuthor,
             developBranchAuthor, developBranchAuthor
            ])
        scriptMock.expectedShRetValueForScript.put('git push origin main develop myVersion', 0)

        scriptMock.expectedDefaultShRetValue = ""
        scriptMock.env.BRANCH_NAME = "myReleaseBranch"
        Git git = new Git(scriptMock)
        GitFlow gitflow = new GitFlow(scriptMock, git)
        gitflow.finishRelease("myVersion", "main")

        scriptMock.allActualArgs.removeAll("echo ")
        scriptMock.allActualArgs.removeAll("git --no-pager show -s --format='%an <%ae>' HEAD")
        int i = 0
        assertEquals("git ls-remote origin refs/tags/myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'", scriptMock.allActualArgs[i++])
        assertEquals("git fetch --all", scriptMock.allActualArgs[i++])
        assertEquals("git log origin/myReleaseBranch..origin/develop --oneline", scriptMock.allActualArgs[i++])
        assertEquals("git checkout myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git reset --hard origin/myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git checkout develop", scriptMock.allActualArgs[i++])
        assertEquals("git reset --hard origin/develop", scriptMock.allActualArgs[i++])
        assertEquals("git checkout main", scriptMock.allActualArgs[i++])
        assertEquals("git reset --hard origin/main", scriptMock.allActualArgs[i++])

        // Author & Email 1 (calls 'git --no-pager...' twice)
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[i++])
        assertAuthor(0, releaseBranchAuthorName, releaseBranchEmail)

        // Author & Email 2 (calls 'git --no-pager...' twice)
        assertEquals("git tag -f -m \"release version myVersion\" myVersion", scriptMock.allActualArgs[i++])
        assertAuthor(1, releaseBranchAuthorName, releaseBranchEmail)

        assertEquals("git checkout develop", scriptMock.allActualArgs[i++])
        // Author & Email 3 (calls 'git --no-pager...' twice)
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[i++])
        assertAuthor(2, releaseBranchAuthorName, releaseBranchEmail)

        assertEquals("git branch -d myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git checkout myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git push origin main develop myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git push --delete origin myReleaseBranch", scriptMock.allActualArgs[i++])
    }

    @Test
    void testThrowsErrorWhenTagAlreadyExists() {
        scriptMock.expectedShRetValueForScript.put('git ls-remote origin refs/tags/myVersion', 'thisIsATag')
        Git git = new Git(scriptMock)
        GitFlow gitflow = new GitFlow(scriptMock, git)
        def err = shouldFail(Exception.class) {
            gitflow.finishRelease("myVersion")
        }
        assertEquals('You cannot build this version, because it already exists.', err.getMessage())
    }

    @Test
    void testThrowsErrorWhenDevelopHasChanged() {
        scriptMock.env.BRANCH_NAME = "branch"
        scriptMock.expectedShRetValueForScript.put("git log origin/branch..origin/develop --oneline", "some changes")
        Git git = new Git(scriptMock)
        GitFlow gitflow = new GitFlow(scriptMock, git)
        def err = shouldFail(Exception.class) {
            gitflow.finishRelease("myVersion")
        }
        assertEquals('There are changes on develop branch that are not merged into release. Please merge and restart process.', err.getMessage())
    }

    void assertAuthor(int withEnvInvocationIndex, String author, String email) {
        def withEnvMap = scriptMock.actualWithEnvAsMap(withEnvInvocationIndex)
        assert withEnvMap['GIT_AUTHOR_NAME'] == author
        assert withEnvMap['GIT_COMMITTER_NAME'] == author
        assert withEnvMap['GIT_AUTHOR_EMAIL'] == email
        assert withEnvMap['GIT_COMMITTER_EMAIL'] == email
    }

    String createGitAuthorString(String author, String email) {
        "${author} <${email}>"
    }

}
