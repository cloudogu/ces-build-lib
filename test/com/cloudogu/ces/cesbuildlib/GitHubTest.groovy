package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class GitHubTest extends GroovyTestCase {
    ScriptMock scriptMock = new ScriptMock()
    Git git = new Git(scriptMock)
    GitHub github = new GitHub(scriptMock, git)

    @Test
    void testPushGitHubPagesBranch() {
        scriptMock.expectedShRetValueForScript.put("git --no-pager show -s --format='%an <%ae>' HEAD", "User Name <user.name@doma.in>")
        scriptMock.expectedShRetValueForScript.put('git remote get-url origin', "https://repo.url")

        github.pushGitHubPagesBranch('website', 'Deploys new version of website')

        assertGitHubPagesBranchToSubFolder('.')
    }

    @Test
    void testPushGitHubPagesBranchToSubFolder() {
        scriptMock.expectedShRetValueForScript.put("git --no-pager show -s --format='%an <%ae>' HEAD", "User Name <user.name@doma.in>")
        scriptMock.expectedShRetValueForScript.put('git remote get-url origin', "https://repo.url")

        github.pushGitHubPagesBranch('website', 'Deploys new version of website', 'some-folder')

        assertGitHubPagesBranchToSubFolder('some-folder')
    }

    private void assertGitHubPagesBranchToSubFolder(String subFolder) {
        assert scriptMock.actualGitArgs.url == "https://repo.url"
        assert scriptMock.actualGitArgs.branch == "gh-pages"

        assert scriptMock.actualDir == '.gh-pages'
        assert scriptMock.allActualArgs.contains("cp -rf ../website/* ${subFolder}".toString())
        assert scriptMock.allActualArgs.contains("mkdir -p ${subFolder}".toString())
        assert scriptMock.allActualArgs.contains('git add .')
        assert scriptMock.allActualArgs.contains('git commit -m "Deploys new version of website"')
        assert scriptMock.actualWithEnv.contains("${'GIT_AUTHOR_NAME=User Name'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_COMMITTER_NAME=User Name'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_AUTHOR_EMAIL=user.name@doma.in'}")
        assert scriptMock.actualWithEnv.contains("${'GIT_COMMITTER_EMAIL=user.name@doma.in'}")
        assert scriptMock.allActualArgs.contains('git push origin gh-pages')
        assert scriptMock.allActualArgs.last == 'rm -rf .gh-pages'
    }
}
