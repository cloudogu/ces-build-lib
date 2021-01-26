package com.cloudogu.ces.cesbuildlib

import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat 

class SCMManagerTest {

  ScriptMock scriptMock = new ScriptMock()
  SCMManager scmm = new SCMManager(scriptMock, "credentialsID")

  @Before
  void init() {
    scmm.repositoryUrl = "scm/repo"
    scriptMock.env.put("GIT_USER", "user")
    scriptMock.env.put("GIT_PASSWORD", "pw")
  }

  @Test
  void "getting all pull requests"() {
    String response = """HTTP/2 200
some: header

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
}"""
    scriptMock.expectedDefaultShRetValue = response
    def prs = scmm.getPullRequests()
    def id = prs["id"]
    assertThat(id.toString()).isEqualTo('[1, 2]')
  }

  @Test
  void "server error yields to unstable build while getting all pull requests"() {
    def response = """HTTP/2 500
some: header

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
}"""
    scriptMock.expectedDefaultShRetValue = response
    scmm.getPullRequests()
    assertThat(scriptMock.unstable)
  }

  @Test
  void "find pull request by title"() {
    def response = """HTTP/2 200
some: header

{
  "_embedded": {
    "pullRequests": [
      {
        "title": "one",
        "id" : "1"
      },
      {
        "title": "Two",
        "id" : "2"
      }
    ]
  }
}"""
    scriptMock.expectedDefaultShRetValue = response
    def prs = scmm.searchPullRequestIdByTitle("one")
    assertThat(prs).isEqualTo('1')
  }

  @Test
  void "did not find pull request by title"() {
    def response = """HTTP/2 200
some: header

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
}"""
    scriptMock.expectedDefaultShRetValue = response
    def prs = scmm.searchPullRequestIdByTitle("3")
    assertThat(prs).isEqualTo("")
  }

  @Test
  void "returns empty string when no pr is found"() {
    def response = """HTTP/2 200
some: header

{
  "_embedded": {
    "pullRequests": []
  }
}"""
    scriptMock.expectedDefaultShRetValue = response
    def prs = scmm.searchPullRequestIdByTitle("just something")
    assertThat(prs).isEqualTo("")
  }

  @Test
  void "sucessfully creating a pull request yields the created prs id"() {
    def response = """
HTTP/2 201
location: https://eine/lange/url/mit/id/12
"""
    scriptMock.expectedDefaultShRetValue = response
    def id = scmm.createPullRequest("source", "target", "title", "description")
    assertThat(id.toString()).isEqualTo("12")
  }

  @Test
  void "error on pull request creation makes build unstable"() {
    def response = """
HTTP/2 500
location: https://eine/lange/url/mit/id/12
"""
    scriptMock.expectedDefaultShRetValue = response
    def id = scmm.createPullRequest("source","target","title","description")
    assertThat(id.toString()).isEqualTo("12")
    assertThat(scriptMock.unstable)
  }

  @Test
  void "successful description update yields to a successful build"() {
    def response = """HTTP/2 200
some: header
"""
    scriptMock.expectedDefaultShRetValue = response
    scmm.updateDescription("123","title","description")
    assertThat(!scriptMock.unstable)
  }

  @Test
  void "error on description update yields to an unstable build"() {
    def response = """HTTP/2 200
some: header
"""
    scriptMock.expectedDefaultShRetValue = response
    scmm.updateDescription("123","title","description")
    assertThat(scriptMock.unstable)
  }

  @Test
  void "successful comment update yields to a successful build"() {
      def response = """HTTP/2 201
some: header
"""
    scriptMock.expectedDefaultShRetValue = response
    scmm.addComment("123","comment")
    assertThat(!scriptMock.unstable)
  }

  @Test
  void "error on comment update yields to an unstable build"() {
      def response = """HTTP/2 500
some: header
"""
    scriptMock.expectedDefaultShRetValue = response
    scmm.addComment("123","comment")
    assertThat(scriptMock.unstable)
  }
}
