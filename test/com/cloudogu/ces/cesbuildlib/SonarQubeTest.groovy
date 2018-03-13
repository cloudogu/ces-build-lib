package com.cloudogu.ces.cesbuildlib

import org.junit.After
import org.junit.Test

class SonarQubeTest {

    def scriptMock = new ScriptMock()
    def mavenMock = new MavenMock(scriptMock)

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
        assert mavenMock.additionalArgs.contains('-Dsonar.branch=develop')
        assert scriptMock.actualSonarQubeEnv == 'sqEnv'
    }

    @Test
    void analyzeWithNoExtraProps() throws Exception {
        scriptMock.env = [
                SONAR_MAVEN_GOAL : 'sonar:sonar',
                SONAR_HOST_URL : 'host',
                SONAR_AUTH_TOKEN: 'auth',
                BRANCH_NAME : 'develop'
        ]

        new SonarQube(scriptMock, 'sqEnv').analyzeWith(mavenMock)
        assert !mavenMock.args.contains('null')
    }

    @Test
    void analyzeWithExclusionsFromMaven() throws Exception {
        String exclusionsFromMaven = 'folder/file'
        scriptMock.files.put('pom.xml', "<properties><sonar.exclusions>${exclusionsFromMaven}</sonar.exclusions></properties>")
        new SonarQube(scriptMock, 'sqEnv').analyzeWith(mavenMock)
        assert mavenMock.args.contains("-Dsonar.exclusions=target/**,$exclusionsFromMaven")
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
                BRANCH_NAME : 'develop'
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
                BRANCH_NAME : 'master'
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
                CHANGE_ID : 'PR-42'
        ]

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs == '-Dsonar.analysis.mode=preview -Dsonar.github.pullRequest=PR-42 '
    }

    @Test
    void analyzePullRequestUpdateGitHub() throws Exception {
        scriptMock.expectedIsPullRequest = true
        scriptMock.env = [
                CHANGE_ID : 'PR-42',
                PASSWORD : 'oauthToken'
        ]
        scriptMock.expectedShRetValue = 'github.com/owner/repo'

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
    void waitForQualityGateNotOk() throws Exception {
        scriptMock.expectedQGate = [status: 'SOMETHING ELSE']

        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()

        assert !qualityGate
    }

    @Test
    void waitForQualityGatePullRequest() throws Exception {
        scriptMock.expectedIsPullRequest = true
        def qualityGate = new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()
        assert qualityGate
    }

    private static class MavenMock extends Maven {
        String args

        MavenMock(scriptMock) {
            super(scriptMock)
        }

        def mvn(String args) {
            this.args = args
        }
    }
}
