package com.cloudogu.ces.cesbuildlib


import groovy.json.JsonOutput
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when 

class SCMManagerTest {

    ScriptMock scriptMock = new ScriptMock()
    SCMManager scmm = new SCMManager(scriptMock, "credentialsID")
    HttpClient httpMock

    def jsonTwoPrs = JsonOutput.toJson([
        _embedded: [
            pullRequests: [
                [
                    title: 'one',
                    id   : '1'
                ],
                [
                    title: 'two',
                    id   : '2'
                ]
            ]
        ]
    ])
    
    @Before
    void init() {
        httpMock = mock(HttpClient.class)
        scmm.http = httpMock
        scmm.repositoryUrl = "scm/repo"
    }

    @Test
    void "getting all pull requests"() {
        when(httpMock.get(any(), any())).thenReturn([
            httpCode: '200',
            body    : jsonTwoPrs.toString()
        ])
        
        def prs = scmm.getPullRequests()
        def id = prs["id"]
        assertThat(id.toString()).isEqualTo('[1, 2]')
    }

    @Test
    void "server error yields to unstable build while getting all pull requests"() {
        when(httpMock.get(any(), any())).thenReturn([
            httpCode: '500',
            body    : jsonTwoPrs.toString()
        ])
        scmm.getPullRequests()
        assertThat(scriptMock.unstable)
    }

    @Test
    void "find pull request by title"() {
        when(httpMock.get(any(), any())).thenReturn([
            httpCode: '200',
            body    : jsonTwoPrs.toString()
        ])
        
        def prs = scmm.searchPullRequestIdByTitle("one")
        assertThat(prs).isEqualTo('1')
    }

    @Test
    void "did not find pull request by title"() {
        when(httpMock.get(any(), any())).thenReturn([
            httpCode: '200',
            body    : jsonTwoPrs.toString()
        ])
        def prs = scmm.searchPullRequestIdByTitle("3")
        assertThat(prs).isEqualTo("")
    }

    @Test
    void "returns empty string when no pr is found"() {
        when(httpMock.get(any(), any())).thenReturn([
            httpCode: '200',
            body    : JsonOutput.toJson([
                _embedded: [
                    pullRequests: []
                    ]
            ])
        ])

        def prs = scmm.searchPullRequestIdByTitle("just something")
        assertThat(prs).isEqualTo("")
    }

    @Test
    void "successfully creating a pull request yields the created prs id"() {
        when(httpMock.post(any(), any(), any())).thenReturn([
            httpCode: '201',
            headers: [ location: 'https://a/long/url/with/id/id/12' ]
        ])
   
        def id = scmm.createPullRequest("source", "target", "title", "description")
        assertThat(id.toString()).isEqualTo("12")
    }

    @Test
    void "error on pull request creation makes build unstable"() {
        when(httpMock.post(any(), any(), any())).thenReturn([
            httpCode: '500',
            headers: [ location: 'https://a/long/url/with/id/id/12' ]
        ])
        
        def id = scmm.createPullRequest("source", "target", "title", "description")
        assertThat(id.toString()).isEqualTo("")
        assertThat(scriptMock.unstable)
    }

    @Test
    void "successful description update yields to a successful build"() {
        when(httpMock.put(any(), any(), any())).thenReturn([
            httpCode: '204',
        ])
        scmm.updateDescription("123", "title", "description")
        assertThat(scriptMock.unstable).isFalse()
    }

    @Test
    void "error on description update yields to an unstable build"() {
        when(httpMock.put(any(), any(), any())).thenReturn([
            httpCode: '500',
        ])
        
        scmm.updateDescription("123", "title", "description")
        assertThat(scriptMock.unstable).isTrue()
    }

    @Test
    void "successful comment update yields to a successful build"() {
        when(httpMock.post(any(), any(), any())).thenReturn([
            httpCode: '201',
        ])
        
        scmm.addComment("123", "comment")
        assertThat(scriptMock.unstable).isFalse()
    }

    @Test
    void "error on comment update yields to an unstable build"() {
        when(httpMock.post(any(), any(), any())).thenReturn([
            httpCode: '500',
        ])
        
        scmm.addComment("123", "comment")
        assertThat(scriptMock.unstable).isTrue()
    }
}
