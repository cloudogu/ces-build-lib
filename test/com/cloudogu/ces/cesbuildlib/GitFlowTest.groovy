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
}
