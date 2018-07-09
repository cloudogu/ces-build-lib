package com.cloudogu.ces.cesbuildlib

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail

class FindHostnameTest extends BasePipelineTest {

    def script
    String errorParam = ""

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        script = loadScript('vars/findHostName.groovy')
        helper.registerAllowedMethod("error", [String.class], { arg ->
            errorParam = arg
            throw new RuntimeException("Mocked error")
        })
    }

    @Test
    void developBranchHttp() {
        script.env = [JENKINS_URL: 'http://jenkins.url:123/jenkins']

        def actualHostname = script.call()

        assert actualHostname == 'jenkins.url'
    }

    @Test
    void developBranchHttps() {
        script.env = [JENKINS_URL: 'https://jenkins.url:123/jenkins']

        def actualHostname = script.call()

        assert actualHostname == 'jenkins.url'
    }

    @Test
    void developBranchInvalid() {
        script.env = [JENKINS_URL: 'df://jenkins.url:123/jenkins']

        def exception = shouldFail {
            script.call()
        }

        assert 'Mocked error' == exception.getMessage()
        errorParam = 'Unable to determine hostname from env.JENKINS_URL. Expecting http(s)://server:port/jenkins'
    }
}