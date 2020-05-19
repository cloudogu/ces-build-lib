package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class GitFlowTest extends GroovyTestCase {
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
        def scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = ""
        scriptMock.env.BRANCH_NAME = "myReleaseBranch"
        Git git = new Git(scriptMock)
        GitFlow gitflow = new GitFlow(scriptMock, git)
        gitflow.finishGitRelease("myVersion")
        scriptMock.allActualArgs.removeAll("echo ")
        assertEquals(20, scriptMock.allActualArgs.size())
        int i = 0
        assertEquals("git ls-remote origin refs/tags/myVersion", scriptMock.allActualArgs[0])
        assertEquals("git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'", scriptMock.allActualArgs[1])
        assertEquals("git fetch --all", scriptMock.allActualArgs[2])
        assertEquals("git log origin/myReleaseBranch..origin/develop --oneline", scriptMock.allActualArgs[3])
        assertEquals("git checkout myReleaseBranch", scriptMock.allActualArgs[4])
        assertEquals("git pull origin myReleaseBranch", scriptMock.allActualArgs[5])
        assertEquals("git checkout develop", scriptMock.allActualArgs[6])
        assertEquals("git pull origin develop", scriptMock.allActualArgs[7])
        assertEquals("git checkout master", scriptMock.allActualArgs[8])
        assertEquals("git pull origin master", scriptMock.allActualArgs[9])
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[10])
        assertEquals("git tag -f -m 'release version myVersion' myVersion", scriptMock.allActualArgs[11])
        assertEquals("git checkout develop", scriptMock.allActualArgs[12])
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[13])
        assertEquals("git branch -d myReleaseBranch", scriptMock.allActualArgs[14])
        assertEquals("git checkout myVersion", scriptMock.allActualArgs[15])
        assertEquals("git push origin master", scriptMock.allActualArgs[16])
        assertEquals("git push origin develop", scriptMock.allActualArgs[17])
        assertEquals("git push origin --tags", scriptMock.allActualArgs[18])
        assertEquals("git push --delete origin myReleaseBranch", scriptMock.allActualArgs[19])

    }
}
