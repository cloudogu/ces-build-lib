package com.cloudogu.ces.cesbuildlib

import org.junit.Test
import static org.junit.Assert.*
import static groovy.test.GroovyAssert.shouldFail

class GitHubTest {
    def testChangelog =
            '''
## [Unreleased]

## [v1.0.0]
### Added
- everything
    
'''
    
    ScriptMock scriptMock = new ScriptMock()

    @Test
    void testPushPagesBranch() {
        Git git = new Git(scriptMock)
        GitHub github = new GitHub(scriptMock, git)
        scriptMock.expectedShRetValueForScript.put('git push origin gh-pages', 0)
        scriptMock.expectedShRetValueForScript.put("git --no-pager show -s --format='%an <%ae>' HEAD", "User Name <user.name@doma.in>")
        scriptMock.expectedShRetValueForScript.put('git remote get-url origin', "https://repo.url")

        github.pushPagesBranch('website', 'Deploys new version of website')

        assertPagesBranchToSubFolder('.', scriptMock)
    }

    @Test
    void testPushPagesBranchToSubFolder() {
        Git git = new Git(scriptMock)
        GitHub github = new GitHub(scriptMock, git)
        scriptMock.expectedShRetValueForScript.put('git push origin gh-pages', 0)
        scriptMock.expectedShRetValueForScript.put("git --no-pager show -s --format='%an <%ae>' HEAD", "User Name <user.name@doma.in>")
        scriptMock.expectedShRetValueForScript.put('git remote get-url origin', "https://repo.url")

        github.pushPagesBranch('website', 'Deploys new version of website', 'some-folder')

        assertPagesBranchToSubFolder('some-folder', scriptMock)
    }

    private void assertPagesBranchToSubFolder(String subFolder, ScriptMock scriptMock) {
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

    @Test
    void testCreateReleaseByChangelog() {
        scriptMock.files.put('CHANGELOG.md', testChangelog)
        scriptMock.expectedShRetValueForScript.put("git remote get-url origin", "myRepoName")
        scriptMock.expectedShRetValueForScript.put("curl -u \$GIT_AUTH_USR:\$GIT_AUTH_PSW --request POST --data '{\"tag_name\": \"v1.0.0\", \"target_commitish\": \"master\", \"name\": \"v1.0.0\", \"body\":\"### Added\\n- everything\"}' --header \"Content-Type: application/json\"  https://api.github.com/repos/myRepoName/releases", "{\"id\": 12345}")
        Git git = new Git(scriptMock, "credentials")
        GitHub github = new GitHub(scriptMock, git)
        Changelog changelog = new Changelog(scriptMock)

        String response=github.createReleaseWithChangelog("v1.0.0", changelog)
        assertEquals(response, "12345")

        assertEquals(2, scriptMock.allActualArgs.size())
        int i = 0;
        assertEquals("git remote get-url origin", scriptMock.allActualArgs[i++])

        String expectedData = """--data '{"tag_name": "v1.0.0", "target_commitish": "master", """ +
            """"name": "v1.0.0", "body":"### Added\\n- everything"}'"""
        String expectedHeader = """--header "Content-Type: application/json"  https://api.github.com/repos/myRepoName/releases"""

        assertEquals("curl -u \$GIT_AUTH_USR:\$GIT_AUTH_PSW --request POST ${expectedData} ${expectedHeader}".toString(), scriptMock.allActualArgs[i++])
    }

    @Test
    void testCreateReleaseByChangelogOnMainBranch() {
        String expectedProductionBranch = "main"

        scriptMock.files.put('CHANGELOG.md', testChangelog)
        scriptMock.expectedShRetValueForScript.put("git remote get-url origin", "myRepoName")
        scriptMock.expectedShRetValueForScript.put("curl -u \$GIT_AUTH_USR:\$GIT_AUTH_PSW --request POST --data '{\"tag_name\": \"v1.0.0\", \"target_commitish\": \"main\", \"name\": \"v1.0.0\", \"body\":\"### Added\\n- everything\"}' --header \"Content-Type: application/json\"  https://api.github.com/repos/myRepoName/releases", "{\"id\": 12345}")

        Git git = new Git(scriptMock, "credentials")
        GitHub github = new GitHub(scriptMock, git)
        Changelog changelog = new Changelog(scriptMock)

        String response=github.createReleaseWithChangelog("v1.0.0", changelog, expectedProductionBranch)
        assertEquals(response, "12345")

        assertEquals(2, scriptMock.allActualArgs.size())
        int i = 0;
        assertEquals("git remote get-url origin", scriptMock.allActualArgs[i++])

        String expectedData = """--data '{"tag_name": "v1.0.0", "target_commitish": "${expectedProductionBranch}", """ +
            """"name": "v1.0.0", "body":"### Added\\n- everything"}'"""
        String expectedHeader = """--header "Content-Type: application/json"  https://api.github.com/repos/myRepoName/releases"""

        assertEquals("curl -u \$GIT_AUTH_USR:\$GIT_AUTH_PSW --request POST ${expectedData} ${expectedHeader}".toString(), scriptMock.allActualArgs[i++])
    }

    @Test
    void testReleaseFailsWithoutCredentials() {
        scriptMock.files.put('CHANGELOG.md', testChangelog)
        scriptMock.expectedShRetValueForScript.put("git remote get-url origin", "myRepoName")
        Git git = new Git(scriptMock)
        GitHub github = new GitHub(scriptMock, git)
        Changelog changelog = new Changelog(scriptMock)

        def exception = shouldFail {
            github.createRelease("v1.0.0", "changes")
        }
        assert exception.getMessage().contains("Unable to create Github release without credentials")

        assertFalse(scriptMock.unstable)
        github.createReleaseWithChangelog("v1.0.0", changelog)
        assertTrue(scriptMock.unstable)
    }

    @Test
    void testAddReleaseAssetFailsWithoutCredentials() {
        scriptMock.files.put('tool.sha256sum.asc', "")
        scriptMock.expectedShRetValueForScript.put("git remote get-url origin", "myRepoName")
        Git git = new Git(scriptMock)
        GitHub github = new GitHub(scriptMock, git)

        assertFalse(scriptMock.unstable)
        github.addReleaseAsset("12345", "tool.sha256sum.asc")
        assertTrue(scriptMock.unstable)
    }

    @Test
    void testAddReleaseAssetWithoutError() {
        scriptMock.files.put('tool.sha256sum.asc', "")
        scriptMock.expectedShRetValueForScript.put("git remote get-url origin", "myRepoName")
        Git git = new Git(scriptMock, "credentials")
        scriptMock.usernamePassword(["username": "password"])
        GitHub github = new GitHub(scriptMock, git)

        assertFalse(scriptMock.unstable)
        github.addReleaseAsset("12345", "tool.sha256sum.asc")
        assertFalse(scriptMock.unstable)
    }
}
