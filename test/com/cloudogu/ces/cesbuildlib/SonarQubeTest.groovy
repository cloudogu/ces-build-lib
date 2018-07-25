package com.cloudogu.ces.cesbuildlib

import org.junit.After
import org.junit.Test
import static groovy.test.GroovyAssert.shouldFail

class SonarQubeTest {

    def scriptMock = new ScriptMock()
    def mavenMock = new MavenMock(scriptMock)

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        SonarQube.metaClass = null
    }

    @Test
    void analyzeWithDeprecatedConstructor() throws Exception {
        analyzeWith(new SonarQube(scriptMock, 'sqEnv'))
    }

    @Test
    void analyzeWithSonarQubeEnv() throws Exception {
        analyzeWith(new SonarQube(scriptMock, [sonarQubeEnv: 'sqEnv']))
    }

    @Test
    void analyzeWithToken() throws Exception {
        def sonarQube = new SonarQube(scriptMock, [token: 'secretTextCred', sonarHostUrl: 'http://ces/sonar'])

        scriptMock.env = [
                SONAR_AUTH_TOKEN: 'auth',
                BRANCH_NAME     : 'develop'
        ]

        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.args ==
                'sonar:sonar -Dsonar.host.url=http://ces/sonar -Dsonar.login=auth '
        assert mavenMock.additionalArgs.contains('-Dsonar.branch=develop')
        assert scriptMock.actualStringArgs['credentialsId'] == 'secretTextCred'
    }

    @Test
    void analyzeWithTokenWithoutHost() throws Exception {
        assertSonarHostUrlError([token: 'secretTextCred'])
    }

    @Test
    void analyzeWithTokenWithEmptyHost() throws Exception {
        assertSonarHostUrlError([token: 'secretTextCred', sonarHostUrl: ''])
    }

    @Test
    void analyzeWithUsernameAndPassword() throws Exception {
        def sonarQube = new SonarQube(scriptMock, [usernamePassword: 'usrPwCred', sonarHostUrl: 'http://ces/sonar'])

        scriptMock.env = [
                USERNAME   : 'usr',
                PASSWORD   : 'pw',
                BRANCH_NAME: 'develop'
        ]

        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.args ==
                'sonar:sonar -Dsonar.host.url=http://ces/sonar -Dsonar.login=usr -Dsonar.password=pw '
        assert mavenMock.additionalArgs.contains('-Dsonar.branch=develop')
        assert scriptMock.actualUsernamePasswordArgs['credentialsId'] == 'usrPwCred'
    }

    @Test
    void analyzeWithUsernameAndPasswordWithoutHost() throws Exception {
        assertSonarHostUrlError([usernamePassword: 'userCred'])
    }

    @Test
    void analyzeWithUsernameAndPasswordWithEmptyHost() throws Exception {
        assertSonarHostUrlError([usernamePassword: 'userCred', sonarHostUrl: ''])
    }

    @Test
    void analyzeWithNothing() throws Exception {
        def exception = shouldFail {
            new SonarQube(scriptMock, [something: 'else']).analyzeWith(mavenMock)
        }

        assert exception.message == "Requires either 'sonarQubeEnv', 'token' or 'usernamePassword' parameter."
    }

    @Test
    void analyzeWithoutBranchName() throws Exception {
        def sonarQube = new SonarQube(scriptMock, [usernamePassword: 'usrPwCred', sonarHostUrl: 'http://ces/sonar'])

        scriptMock.env = [
                USERNAME: 'usr',
                PASSWORD: 'pw',
        ]

        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.args ==
                'sonar:sonar -Dsonar.host.url=http://ces/sonar -Dsonar.login=usr -Dsonar.password=pw '
        assert !mavenMock.additionalArgs.contains('-Dsonar.branch')
        assert scriptMock.actualUsernamePasswordArgs['credentialsId'] == 'usrPwCred'
    }

    void analyzeWith(SonarQube sonarQube) throws Exception {
        scriptMock.env = [
                SONAR_MAVEN_GOAL : 'sonar:sonar',
                SONAR_HOST_URL   : 'host',
                SONAR_AUTH_TOKEN : 'auth',
                SONAR_EXTRA_PROPS: '-DextraKey=extraValue',
                BRANCH_NAME      : 'develop'
        ]

        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.args ==
                'sonar:sonar -Dsonar.host.url=host -Dsonar.login=auth -DextraKey=extraValue'
        assert mavenMock.additionalArgs.contains('-Dsonar.branch=develop')
        assert scriptMock.actualSonarQubeEnv == 'sqEnv'
    }

    @Test
    void analyzeWithNoExtraProps() throws Exception {
        scriptMock.env = [
                SONAR_MAVEN_GOAL: 'sonar:sonar',
                SONAR_HOST_URL  : 'host',
                SONAR_AUTH_TOKEN: 'auth',
                BRANCH_NAME     : 'develop'
        ]

        new SonarQube(scriptMock, 'sqEnv').analyzeWith(mavenMock)
        assert !mavenMock.args.contains('null')
    }

    @Test
    void analyzeIgnoreBranches() throws Exception {
        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.isIgnoringBranches = true
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs == ''
    }

    @Test
    void analyzeWithPaidVersion() throws Exception {
        scriptMock.env = [
                BRANCH_NAME: 'develop'
        ]

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        mavenMock.additionalArgs = '-X'
        sonarQube.isUsingBranchPlugin = true
        sonarQube.analyzeWith(mavenMock)

        def additionalArgs = mavenMock.additionalArgs.split("\\s+")
        assert additionalArgs.size() == 3
        assert additionalArgs[0].trim() == '-X'
        assert additionalArgs[1].trim() == '-Dsonar.branch.name=develop'
        assert additionalArgs[2].trim() == '-Dsonar.branch.target=master'
    }

    @Test
    void analyzeWithPaidVersionOnMasterBranch() throws Exception {
        scriptMock.env = [
                BRANCH_NAME: 'master'
        ]

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        mavenMock.additionalArgs = '-X'
        sonarQube.isUsingBranchPlugin = true
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs == '-X -Dsonar.branch.name=master '
    }

    @Test
    void analyzePullRequest() throws Exception {
        scriptMock.expectedIsPullRequest = true
        scriptMock.env = [
                CHANGE_ID: 'PR-42'
        ]

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs == '-Dsonar.analysis.mode=preview -Dsonar.github.pullRequest=PR-42 '
    }

    @Test
    void analyzePullRequestUpdateGitHub() throws Exception {
        scriptMock.expectedIsPullRequest = true
        scriptMock.env = [
                CHANGE_ID: 'PR-42',
                PASSWORD : 'oauthToken'
        ]
        scriptMock.expectedDefaultShRetValue = 'github.com/owner/repo'

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.updateAnalysisResultOfPullRequestsToGitHub('ghCredentials')
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs ==
                '-Dsonar.analysis.mode=preview -Dsonar.github.pullRequest=PR-42 -Dsonar.github.repository=owner/repo -Dsonar.github.oauth=oauthToken '
    }

    @Test
    void waitForQualityGate() throws Exception {
        scriptMock.expectedQGate = [status: 'OK']

        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()

        assert qualityGate
    }

    @Test
    void waitForQualityGateWithToken() throws Exception {

        def exception = shouldFail {
            new SonarQube(scriptMock, [tokem: 'sometoken', sonarHostUrl: 'http://ces/sonar'])
                    .waitForQualityGateWebhookToBeCalled()
        }

        assert exception.message == "waitForQualityGate will only work when using the SonarQube Plugin for Jenkins, via the 'sonarQubeEnv' parameter"
    }

    @Test
    void waitForQualityGateWithUsernameAndPassword() throws Exception {

        def exception = shouldFail {
            new SonarQube(scriptMock, [usernamePassword: 'usrPwCred', sonarHostUrl: 'http://ces/sonar'])
                    .waitForQualityGateWebhookToBeCalled()
        }

        assert exception.message == "waitForQualityGate will only work when using the SonarQube Plugin for Jenkins, via the 'sonarQubeEnv' parameter"
    }

    @Test
    void waitForQualityGateNotOk() throws Exception {
        scriptMock.expectedQGate = [status: 'SOMETHING ELSE']

        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()

        assert !qualityGate
    }

    @Test
    void waitForQualityGatePullRequest() throws Exception {
        scriptMock.expectedQGate = [status: 'SOMETHING ELSE']
        scriptMock.expectedIsPullRequest = true
        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()
        assert qualityGate
    }

    private void assertSonarHostUrlError(configMapg) {
        def exception = shouldFail {
            new SonarQube(scriptMock, configMapg).analyzeWith(mavenMock)
        }

        assert exception.message == "Missing required 'sonarHostUrl' parameter."
    }
}
