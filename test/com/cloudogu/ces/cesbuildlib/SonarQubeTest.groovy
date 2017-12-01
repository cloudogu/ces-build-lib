package com.cloudogu.ces.cesbuildlib

import org.junit.After
import org.junit.Test

class SonarQubeTest {

    def scriptMock = new ScriptMock()
    def mavenMock = new MavenMock()

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        SonarQube.metaClass = null
    }

    @Test
    void analyzeWith() throws Exception {
        scriptMock.env = [
                SONAR_MAVEN_GOAL : 'sonar:sonar',
                SONAR_HOST_URL : 'host',
                SONAR_AUTH_TOKEN: 'auth',
                SONAR_EXTRA_PROPS: '-DextraKey=extraValue',
                BRANCH_NAME : 'develop'
        ]

        new SonarQube(scriptMock, 'sqEnv').analyzeWith(mavenMock)

        assert mavenMock.args ==
                'sonar:sonar -Dsonar.host.url=host -Dsonar.login=auth -DextraKey=extraValue -Dsonar.exclusions=target/**'
        assert mavenMock.additionalArgs == '-Dsonar.branch=develop'
        assert scriptMock.sonarQubeEnv == 'sqEnv'
    }

    @Test
    void analyzeWithPaidVersion() throws Exception {
        scriptMock.env = [
                BRANCH_NAME : 'develop'
        ]

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.isUsingBranchPlugin = true
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs == '-Dsonar.branch.name=develop -Dsonar.branch.target=master'
    }

    @Test
    void analyzePullRequest() throws Exception {
        scriptMock.isPullRequest = true
        scriptMock.env = [
                CHANGE_ID : 'PR-42'
        ]

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs == '-Dsonar.analysis.mode=preview -Dsonar.github.pullRequest=PR-42 '
    }

    @Test
    void analyzePullRequestUpdateGitHub() throws Exception {
        scriptMock.isPullRequest = true
        scriptMock.env = [
                CHANGE_ID : 'PR-42',
                PASSWORD : 'oauthToken'
        ]
        scriptMock.shRetValue = 'github.com/owner/repo'

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.updateAnalysisResultOfPullRequestsToGitHub('ghCredentials')
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs ==
                '-Dsonar.analysis.mode=preview -Dsonar.github.pullRequest=PR-42 -Dsonar.github.repository=owner/repo -Dsonar.github.oauth=oauthToken '
    }

    @Test
    void waitForQualityGate() throws Exception {
        scriptMock.qGate = [ status : 'OK']

        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()

        assert qualityGate
    }

    @Test
    void waitForQualityGateNotOk() throws Exception {
        scriptMock.qGate = [ status : 'SOMETHING ELSE']

        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()

        assert !qualityGate
    }

    @Test
    void waitForQualityGatePullRequest() throws Exception {
        scriptMock.isPullRequest = true
        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()
        assert qualityGate
    }

    private class ScriptMock {
        String sonarQubeEnv
        boolean isPullRequest = false
        def qGate
        def env = [ : ]
        def shRetValue

        String sh(Map<String, String> params) {
            shRetValue
        }

        boolean isPullRequest() {
            return isPullRequest
        }

        void timeout(Map params, closure) {
            closure.call()
        }

        def waitForQualityGate() {
            return qGate
        }

        void withSonarQubeEnv(String sonarQubeEnv, Closure closure) {
            this.sonarQubeEnv = sonarQubeEnv
            closure.call()
        }

        void withCredentials(List args, Closure closure) {
            closure.call()
        }

        void string(Map args) {}

        void echo(String msg) {}
    }

    private static class MavenMock extends Maven {
        String args

        MavenMock() {
            super(new Object())
        }

        def mvn(String args) {
            this.args = args
        }
    }
}
