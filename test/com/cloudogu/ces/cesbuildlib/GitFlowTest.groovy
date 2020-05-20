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
        assertEquals(30, scriptMock.allActualArgs.size())
        int i = 0
        assertEquals("git ls-remote origin refs/tags/myVersion > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git config 'remote.origin.fetch' '+refs/heads/*:refs/remotes/origin/*'", scriptMock.allActualArgs[i++])
        assertEquals("git fetch --all > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git log origin/myReleaseBranch..origin/develop --oneline > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git checkout myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git pull origin myReleaseBranch > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git checkout develop", scriptMock.allActualArgs[i++])
        assertEquals("git pull origin develop > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git checkout master", scriptMock.allActualArgs[i++])
        assertEquals("git pull origin master > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git tag -f -m 'release version myVersion' myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git checkout develop", scriptMock.allActualArgs[i++])
        assertEquals("git merge --no-ff myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git branch -d myReleaseBranch", scriptMock.allActualArgs[i++])
        assertEquals("git checkout myVersion", scriptMock.allActualArgs[i++])
        assertEquals("git push origin master > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git push origin develop > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git push origin --tags > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
        assertEquals("git push --delete origin myReleaseBranch > output", scriptMock.allActualArgs[i++])
        assertEquals("cat output", scriptMock.allActualArgs[i++])
    }

    @Test
    void testThrowsErrorWhenTagAlreadyExists() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedDefaultShRetValue = "thisIsATag"
        Git git = new Git(scriptMock)
        GitFlow gitflow = new GitFlow(scriptMock, git)
        String err = shouldFail(Exception.class) {
            gitflow.finishGitRelease("myVersion")
        }
        assertEquals("You cannot build this version, because it already exists.", err)
    }

    @Test
    void testThrowsErrorWhenDevelopHasChanged() {
        def scriptMock = new ScriptMock()
        scriptMock.env.BRANCH_NAME = "branch"
        scriptMock.expectedShRetValueForScript.put("git log origin/branch..origin/develop --oneline", "error")
        scriptMock.expectedShRetValueForScript.put("cat output", ["", "", "some changes"])
        Git git = new Git(scriptMock)
        GitFlow gitflow = new GitFlow(scriptMock, git)
        String err = shouldFail(Exception.class) {
            gitflow.finishGitRelease("myVersion")
        }
        assertEquals("There are changes on develop branch that are not merged into release. Please merge and restart process.", err)
    }
}
