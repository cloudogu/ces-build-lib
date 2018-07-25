package com.cloudogu.ces.cesbuildlib

import org.junit.After
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.assertEquals

class SonarCloudTest {

    def scriptMock = new ScriptMock()
    def mavenMock = new MavenMock(scriptMock)

    def sonarCloud = new SonarCloud(scriptMock, [sonarQubeEnv: 'sonarcloud.io'])

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        SonarQube.metaClass = null
    }

    @Test
    void regularAnalysis() {

        doRegularAnalysis()

    }

    private void doRegularAnalysis() {
        scriptMock.env = [
                SONAR_MAVEN_GOAL : 'sonar:sonar',
                SONAR_HOST_URL   : 'host',
                SONAR_AUTH_TOKEN : 'auth',
                SONAR_EXTRA_PROPS: '-DextraKey=extraValue',
                BRANCH_NAME      : 'develop'
        ]

        sonarCloud.analyzeWith(mavenMock)

        assert sonarCloud.isUsingBranchPlugin
        assert mavenMock.args ==
                'sonar:sonar -Dsonar.host.url=host -Dsonar.login=auth -DextraKey=extraValue'
        assert scriptMock.actualSonarQubeEnv == 'sonarcloud.io'
    }

    @Test
    void pullRequestAnalysis() {
        scriptMock.expectedIsPullRequest = true
        scriptMock.env = [
                CHANGE_ID : 'PR-42',
                CHANGE_TARGET : 'develop',
                CHANGE_BRANCH : 'feature/something'
        ]
        scriptMock.expectedDefaultShRetValue = 'github.com/owner/repo'

        sonarCloud.analyzeWith(mavenMock)

        assertEquals(
                '-Dsonar.pullrequest.base=develop -Dsonar.pullrequest.branch=feature/something ' +
                '-Dsonar.pullrequest.key=PR-42 -Dsonar.pullrequest.provider=GitHub ' +
                '-Dsonar.pullrequest.github.repository=owner/repo ',
                mavenMock.additionalArgs)
    }

    @Test
    void waitForQualityGatePullRequest() throws Exception {
        scriptMock.expectedQGate = [status: 'OK']
        scriptMock.expectedIsPullRequest = true
        def qualityGate = sonarCloud.waitForQualityGateWebhookToBeCalled()
        assert qualityGate
    }

    @Test
    void sonarOrganizationMandatoryForUsernameAndPassword() {
        def exception = shouldFail {
            new SonarCloud(scriptMock, [usernamePassword: 'secretTextCred', sonarHostUrl: 'http://ces/sonar']).analyzeWith(mavenMock)
        }

        assert exception.message == "Missing required 'sonarOrganization' parameter."
    }

    @Test
    void sonarOrganizationMandatoryForToken() {
        def exception = shouldFail {
            new SonarCloud(scriptMock, [token: 'secretTextCred', sonarHostUrl: 'http://ces/sonar']).analyzeWith(mavenMock)
        }

        assert exception.message == "Missing required 'sonarOrganization' parameter."
    }

    @Test
    void setSonarOrganizationIfPresent() {
        sonarCloud = new SonarCloud(scriptMock, [sonarQubeEnv: 'sonarcloud.io', sonarOrganization: 'org'])

        doRegularAnalysis()

        assert mavenMock.additionalArgs.endsWith(' -Dsonar.organization=org ')
    }

}
