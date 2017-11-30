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
        scriptMock.metaClass.SONAR_MAVEN_GOAL = 'sonar:sonar'
        scriptMock.metaClass.SONAR_HOST_URL = 'host'
        scriptMock.metaClass.SONAR_AUTH_TOKEN = 'auth'
        scriptMock.metaClass.SONAR_EXTRA_PROPS = '-DextraKey=extraValue'

        new SonarQube(scriptMock, 'sqEnv').analyzeWith(mavenMock)

        assert mavenMock.args == 'sonar:sonar -Dsonar.host.url=host -Dsonar.login=auth -DextraKey=extraValue -Dsonar.exclusions=target/**'
        assert scriptMock.sonarQubeEnv == 'sqEnv'
    }

    @Test
    void waitForQualityGate() throws Exception {
        scriptMock.isPullRequest = false
        scriptMock.qGate = [ status : 'OK']

        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGate()

        assert qualityGate
    }

    @Test
    void waitForQualityGateNotOk() throws Exception {
        scriptMock.isPullRequest = false
        scriptMock.qGate = [ status : 'SOMETHING ELSE']

        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGate()

        assert !qualityGate
    }

    @Test
    void waitForQualityGatePullRequest() throws Exception {
        scriptMock.isPullRequest = true
        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGate()
        assert qualityGate
    }

    private class ScriptMock {
        String sonarQubeEnv
        boolean isPullRequest
        def qGate

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
