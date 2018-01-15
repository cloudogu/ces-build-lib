package com.cloudogu.ces.cesbuildlib

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

class IsPullRequestTest extends BasePipelineTest {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void isPullRequest() {

        def script = loadScript('vars/isPullRequest.groovy')
        script.env = [CHANGE_ID: 'PR-42']

        def isPullRequest = script.call()

        assert isPullRequest == true
    }

    @Test
    void isNotPullRequestChangeIdNull() {

        def script = loadScript('vars/isPullRequest.groovy')
        script.env = [CHANGE_ID: null]

        def isPullRequest = script.call()

        assert isPullRequest == false
    }

    @Test
    void isNotPullRequestChangeIdEmpty() {

        def script = loadScript('vars/isPullRequest.groovy')
        script.env = [CHANGE_ID: '']

        def isPullRequest = script.call()

        assert isPullRequest == false
    }
}