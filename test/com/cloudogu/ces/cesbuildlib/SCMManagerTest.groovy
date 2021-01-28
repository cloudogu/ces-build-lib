package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when 

class SCMManagerTest {

    ScriptMock scriptMock = new ScriptMock()
    String repo = 'scm/repo'
    String baseUrl = 'http://ho.st/scm'
    SCMManager scmm = new SCMManager(scriptMock, baseUrl, "credentialsID")
    HttpClient httpMock

    def slurper = new JsonSlurper()
    
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
    }

    @Test
    void "find pull request by title"() {
        when(httpMock.get(any(), any())).then({ invocation ->
            assert invocation.getArguments()[0] == 'http://ho.st/scm/api/v2/pull-requests/scm/repo'
            assert invocation.getArguments()[1] == 'application/vnd.scmm-pullRequestCollection+json;v=2'

            return [
                httpCode: '200',
                body    : jsonTwoPrs.toString()
            ]
        })
        
        def prs = scmm.searchPullRequestIdByTitle(repo, "one")
        assertThat(prs).isEqualTo('1')
    }

    @Test
    void "did not find pull request by title"() {
        when(httpMock.get(any(), any())).thenReturn([
                httpCode: '200',
                body    : jsonTwoPrs.toString()
        ])
        def prs = scmm.searchPullRequestIdByTitle(repo, "3")
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

        def prs = scmm.searchPullRequestIdByTitle(repo, "just something")
        assertThat(prs).isEqualTo("")
    }

    @Test
    void "successfully creating a pull request yields the created prs id"() {
        def expected = [
            title      : 'ti',
            description: 'd',
            source     : 's',
            target     : 'ta'
        ]
        
        when(httpMock.post(any(), any(), any())).then({ invocation ->
            assert invocation.getArguments()[0] == 'http://ho.st/scm/api/v2/pull-requests/scm/repo'
            assert invocation.getArguments()[1] == 'application/vnd.scmm-pullRequest+json;v=2'
            assert invocation.getArguments()[2] == JsonOutput.toJson(expected)

            return [
                httpCode: '201',
                headers: [ location: 'https://a/long/url/with/id/id/12' ]
            ]
        })
   
        def id = scmm.createPullRequest(repo, expected.source, expected.target, expected.title, expected.description)
        assertThat(id.toString()).isEqualTo('12')
    }

    @Test
    void "error on pull request creation makes build unstable"() {
        when(httpMock.post(any(), any(), any())).thenReturn([
            httpCode: '500',
            headers: [ location: 'https://a/long/url/with/id/id/12' ]
        ])
        
        def id = scmm.createPullRequest(repo, 'source', 'target', 'title', 'description')
        assertThat(id.toString()).isEqualTo("")
        assertThat(scriptMock.unstable)
    }

    @Test
    void "successful description update yields to a successful build"() {
        def expectedTitle = 'title'
        def expectedDescription = 'description'

        when(httpMock.put(any(), any(), any())).then({ invocation ->
            assert invocation.getArguments()[0] == 'http://ho.st/scm/api/v2/pull-requests/scm/repo/123'
            assert invocation.getArguments()[1] == 'application/vnd.scmm-pullRequest+json;v=2'
            def body = slurper.parseText(invocation.getArguments()[2])
            assert body.title == expectedTitle
            assert body.description == expectedDescription
            
            return [ httpCode: '204' ]
        })
        scmm.updatePullRequest(repo, '123', expectedTitle, expectedDescription)
        assertThat(scriptMock.unstable).isFalse()
    }

    @Test
    void "error on description update yields to an unstable build"() {
        when(httpMock.post(any(), any(), any())).then({ invocation ->
            return [ httpCode: '500' ]
        })

        scmm.updatePullRequest(repo, '123', 'title', 'description')
        assertThat(scriptMock.unstable).isTrue()
    }

    @Test
    void "successful comment update yields to a successful build"() {
        String expectedComment = 'com123'
        when(httpMock.post(any(), any(), any())).then({ invocation ->
            assert invocation.getArguments()[0] == 'http://ho.st/scm/api/v2/pull-requests/scm/repo/123/comments'
            assert invocation.getArguments()[1] == 'application/json'
            assert slurper.parseText(invocation.getArguments()[2]).comment == expectedComment
            return [
                httpCode: '201'
            ]
        })
        
        scmm.addComment(repo,'123', expectedComment)
        assertThat(scriptMock.unstable).isFalse()
    }

    @Test
    void "error on comment update yields to an unstable build"() {
        when(httpMock.post(any(), any(), any())).thenReturn([
            httpCode: '500'
        ])
        
        scmm.addComment(repo,'123', 'comment')
        assertThat(scriptMock.unstable).isTrue()
    }
}
