package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import static groovy.test.GroovyAssert.shouldFail

class SonarQubeTest {

    def scriptMock = new ScriptMock()
    def mavenMock = new MavenMock(scriptMock)

    @BeforeEach
    void setup()  {
        mavenMock.mockedArtifactId = "ces-build-lib"
        mavenMock.mockedGroupId = "com.cloudogu.ces"
        mavenMock.mockedName = "ces build lib"
    }

    @AfterEach
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

        def branchName = 'develop.Or:somehing-completely_.different'
        scriptMock.env = [
                SONAR_AUTH_TOKEN: 'auth',
                BRANCH_NAME     : branchName
        ]

        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.args ==
                'sonar:sonar -Dsonar.host.url=http://ces/sonar -Dsonar.login=auth '
        assertBranchName(branchName, mavenMock)
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
        assertBranchName('develop', mavenMock)
        assert scriptMock.actualUsernamePasswordArgs[0]['credentialsId'] == 'usrPwCred'
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
        assert scriptMock.actualUsernamePasswordArgs[0]['credentialsId'] == 'usrPwCred'
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
        assertBranchName('develop', mavenMock)
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
    void analyzeWithBranchContainCharsNotValidForProjectKey() throws Exception {
        String branchName = 'feature/abc'
        // Use custom maven mock because the attributes are read only
        def mavenMock = new MavenMock(scriptMock)
        mavenMock.mockedArtifactId = "ces/build/lib"
        mavenMock.mockedGroupId = "com.cloudogu.ces"
        mavenMock.mockedName = "ces build lib"
        String projectKey = 'ces_build_lib'
        String projectName = "ces/build/lib"
        
        def sonarQube = new SonarQube(scriptMock, [usernamePassword: 'usrPwCred', sonarHostUrl: 'http://ces/sonar'])
        scriptMock.env = [
                SONAR_AUTH_TOKEN: 'auth',
                BRANCH_NAME     : branchName
        ]

        sonarQube.analyzeWith(mavenMock)
        
        assertBranchName(branchName, mavenMock)
        assertProjectKey(projectKey, mavenMock)
        assertProjectName(projectName, mavenMock)
    }

    @Test
    void analyzeWithBranchPlugin() throws Exception {
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
    void analyzeWithBranchPluginOnMasterBranch() throws Exception {
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
    void analyzeWithBranchPluginOnConfiguredIntegrationBranch() throws Exception {
        scriptMock.env = [
                BRANCH_NAME: 'develop'
        ]

        def sonarQube = new SonarQube(scriptMock, ['sonarQubeEnv': 'sqEnv', 'integrationBranch': 'develop'])
        mavenMock.additionalArgs = '-X'
        sonarQube.isUsingBranchPlugin = true
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs == '-X -Dsonar.branch.name=develop '
    }

    @Test
    void analyzeWithBranchPluginWithConfiguredIntegrationBranch() throws Exception {
        scriptMock.env = [
                BRANCH_NAME: 'feature'
        ]

        def sonarQube = new SonarQube(scriptMock, ['sonarQubeEnv': 'sqEnv', 'integrationBranch': 'develop'])
        mavenMock.additionalArgs = '-X'
        sonarQube.isUsingBranchPlugin = true
        sonarQube.analyzeWith(mavenMock)

        def additionalArgs = mavenMock.additionalArgs.split("\\s+")
        assert additionalArgs.size() == 3
        assert additionalArgs[0].trim() == '-X'
        assert additionalArgs[1].trim() == '-Dsonar.branch.name=feature'
        assert additionalArgs[2].trim() == '-Dsonar.branch.target=develop'
    }
    
    @Test
    void analyzeWithBranchPluginOnPullRequest() throws Exception {
        scriptMock.env = [
                BRANCH_NAME: 'PR-42',
                CHANGE_TARGET: 'develop'
        ]

        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        mavenMock.additionalArgs = '-X'
        sonarQube.isUsingBranchPlugin = true
        sonarQube.analyzeWith(mavenMock)

        assert mavenMock.additionalArgs.startsWith('-X -Dsonar.branch.name=PR-42')
        assert mavenMock.additionalArgs.endsWith(' -Dsonar.branch.target=develop ')
    }

    @Test
    void analyzeWithBranchCommunityBranchPluginOnPullRequest() throws Exception {
        def branchName = "PR-42"
        def changeID = "123"
        def changeTarget = "develop"
        scriptMock.env = [
            CHANGE_BRANCH: branchName,
            CHANGE_TARGET: changeTarget,
            CHANGE_ID: changeID,
        ]

        def sonarQube = new SonarQube(scriptMock, ["sonarQubeEnv": "env"])
        sonarQube.analyzeWith(mavenMock)

        assertProjectName(mavenMock.artifactId, mavenMock)
        assertProjectKey(mavenMock.artifactId, mavenMock)
        assert mavenMock.additionalArgs.contains("-Dsonar.pullrequest.key=${changeID}")
        assert mavenMock.additionalArgs.contains("-Dsonar.pullrequest.branch=${branchName}")
        assert mavenMock.additionalArgs.contains("-Dsonar.pullrequest.base=${changeTarget}")
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
    void waitForQualityGateWithDefaultTimeout() {
        scriptMock.expectedQGate = [status: 'OK']

        new SonarQube(scriptMock, 'sqEnv').waitForQualityGateWebhookToBeCalled()

        assert scriptMock.actualTimeoutParams.time == 2
        assert scriptMock.actualTimeoutParams.unit == 'MINUTES'
    }

    @Test
    void waitForQualityGateWithConfiguredTimeout() {
        scriptMock.expectedQGate = [status: 'OK']
        
        def sonarQube = new SonarQube(scriptMock, 'sqEnv')
        sonarQube.timeoutInMinutes = 10
        sonarQube.waitForQualityGateWebhookToBeCalled()

        assert scriptMock.actualTimeoutParams.time == 10
        assert scriptMock.actualTimeoutParams.unit == 'MINUTES'
    }

    private void assertSonarHostUrlError(configMapg) {
        def exception = shouldFail {
            new SonarQube(scriptMock, configMapg).analyzeWith(mavenMock)
        }

        assert exception.message == "Missing required 'sonarHostUrl' parameter."
    }

    static void assertProjectKey(String projectKey, MavenMock mavenMock) {
        assert mavenMock.additionalArgs.contains("-Dsonar.projectKey=${projectKey}")
    }

    static void assertProjectName(String projectName, MavenMock mavenMock) {
        assert mavenMock.additionalArgs.contains("-Dsonar.projectName=${projectName}")
    }

    static void assertBranchName(String branchName, MavenMock mavenMock) {
        assert mavenMock.additionalArgs.contains("-Dsonar.branch.name=${branchName}")
    }
}
