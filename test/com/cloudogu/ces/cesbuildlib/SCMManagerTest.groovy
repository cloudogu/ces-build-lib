package com.cloudogu.ces.cesbuildlib

import static org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test



class SCMManagerTest {

  ScriptMock scriptMock = new ScriptMock()
  SCMManager scmm = new SCMManager(scriptMock, "credentialsID")

  @Before
  void init() {
    scmm.repositoryUrl = "scm/repo"
    scriptMock.env.put("GIT_USER", "user")
    scriptMock.env.put("GIT_PASSWORD", "pw")
  }

  @After
  void tearDown() throws Exception {
    // always reset metaClass after messing with it to prevent changes from leaking to other tests
    SCMManager.metaClass = null
  }

  @Test
  void "getting all pull requests"() {
    def shScript = """curl -w "%{http_code}" -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequestCollection+json;v=2' https://scm/repo"""
    String response = """
{
  "_embedded": {
    "pullRequests": [
      {
        "id": "1"
      },
      {
        "id": "2"
      }
    ]
  }
}200"""
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    def prs = scmm.getPullRequests()
    def id = prs["id"]
    assertThat(id.toString()).isEqualTo('[1, 2]')
  }

  @Test
  void "server error yields to unstable build while getting all pull requests"() {
    def shScript = """curl -w "%{http_code}" -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequestCollection+json;v=2' https://scm/repo"""
    def response = """
{
  "_embedded": {
    "pullRequests": [
      {
        "id": "1"
      },
      {
        "id": "2"
      }
    ]
  }
}500"""
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    scmm.getPullRequests()
    assertThat(scriptMock.unstable)
  }

  @Test
  void "find pull request by title"() {
    def shScript = """curl -w "%{http_code}" -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequestCollection+json;v=2' https://scm/repo"""
    def response = """
{
  "_embedded": {
    "pullRequests": [
      {
        "title": "1"
      },
      {
        "title": "2"
      }
    ]
  }
}200"""
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    def prs = scmm.searchPullRequestByTitle("1")
    def title = prs["title"]
    assertThat(title.toString()).isEqualTo('1')
  }

  @Test
  void "did not find pull request by title"() {
    def shScript = """curl -w "%{http_code}" -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequestCollection+json;v=2' https://scm/repo"""
    def response = """
{
  "_embedded": {
    "pullRequests": [
      {
        "title": "1"
      },
      {
        "title": "2"
      }
    ]
  }
}200"""
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    def prs = scmm.searchPullRequestByTitle("3")
    assertThat(prs).isEqualTo(null)
  }

  @Test
  void "returns null when no pr is found"() {
    def shScript = """curl -w "%{http_code}" -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequestCollection+json;v=2' https://scm/repo"""
    def response = """
{
  "_embedded": {
    "pullRequests": []
  }
}200"""
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    def prs = scmm.searchPullRequestByTitle("3")
    assertThat(prs).isEqualTo(null)
  }

  @Test
  void "sucessfully creating a pull request yields the created prs id"() {
    def shScript = """curl -i -X POST -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '{"title": "title", "description": "description", "source": "source", "target": "target"}' https://scm/repo"""
    def response = """
HTTP/2 201
location: https://eine/lange/url/mit/id/12
"""
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    def id = scmm.createPullRequest("source", "target", "title", "description")
    assertThat(id.toString()).isEqualTo("12")
  }

  @Test
  void "error on pull request creation makes build unstable"() {
    def shScript = """curl -i -X POST -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '{"title": "title", "description": "description", "source": "source", "target": "target"}' https://scm/repo"""
    def response = """
HTTP/2 500
location: https://eine/lange/url/mit/id/12
"""
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    def id = scmm.createPullRequest("source","target","title","description")
    assertThat(id.toString()).isEqualTo("12")
    assertThat(scriptMock.unstable)
  }

  @Test
  void "successful description update yields to a successful build"() {
    def shScript = """curl -X PUT -w "%{http_code}" -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '{"title": "title","description": "description"}' https://scm/repo/123"""
    def response = "204"
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    scmm.updateDescription("123","title","description")
    assertThat(!scriptMock.unstable)
  }

  @Test
  void "error on description update yields to an unstable build"() {
    def data = """{"title": "title","description": "description"}"""
    def shScript = """curl -X PUT -w "%{http_code}" -u user:pw -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '{"title": "title","description": "description"}' https://scm/repo/123"""
    def response = "500"
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    scmm.updateDescription("123","title","description")
    assertThat(scriptMock.unstable)
  }

  @Test
  void "successful comment update yields to a successful build"() {
    def shScript = """curl -X POST -w "%{http_code}" -u user:pw -H 'Content-Type: application/json' -d '{"comment": "comment"}' https://scm/repo/123/comments"""
    def response = "201"
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    scmm.addComment("123","comment")
    assertThat(!scriptMock.unstable)
  }

  @Test
  void "error on comment update yields to an unstable build"() {
    def shScript = """curl -X POST -w "%{http_code}" -u user:pw -H 'Content-Type: application/json' -d '{"comment": "comment"}' https://scm/repo/123/comments"""
    def response = "500"
    scriptMock.expectedDefaultShRetValue = 0
    scriptMock.expectedShRetValueForScript.put(shScript, response)
    scmm.addComment("123","comment")
    assertThat(scriptMock.unstable)
  }
}

