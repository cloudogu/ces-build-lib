package com.cloudogu.ces.cesbuildlib

import com.cloudogu.ces.cesbuildlib.Maven.DeployGoal
import org.junit.After
import org.junit.Before
import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.assertEquals

class MavenTest {
    private static final String EOL = System.getProperty("line.separator")

    static final EXPECTED_PWD = "/home/jenkins/workspaces/NAME"
    def expectedDeploymentGoalWithStaging =
            'org.sonatype.plugins:nexus-staging-maven-plugin:deploy -Dmaven.deploy.skip=true ' +
            '-DserverId=expectedId -DnexusUrl=https://expected.url -DautoReleaseAfterClose=true '

    def scriptMock = new ScriptMock()
    def mvn = new MavenForTest(scriptMock)
    String mvnArgs = null

    @Before
    void setup() {
        scriptMock.expectedPwd = EXPECTED_PWD
    }

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        Maven.metaClass = null
    }

    @Test
    void testCall() throws Exception {
        def result = mvn "test"
        assertEquals("test", result)
    }

    @Test
    void testCallWithMirrors() throws Exception {
        mvn.useMirrors([name: 'n1', mirrorOf: 'm1', url: 'u1'], 
                       [name: 'n2', mirrorOf: 'm2', url: 'u2'])
        
        def result = mvn "test"
        
        assertEquals("test", result)

        assert scriptMock.writeFileParams.size() == 1
        def actualSettingsXml = scriptMock.writeFileParams.get(0)['text']

        for (int mirrorNumber = 1; mirrorNumber <= 2; mirrorNumber++) {

            def expectedXml = "<mirror><name>n${mirrorNumber}</name>" +
                "<mirrorOf>m${mirrorNumber}</mirrorOf>" +
                "<url>u${mirrorNumber}</url>" +
                '</mirror>'
            assert actualSettingsXml.contains(expectedXml)
        }
    }
    
    @Test
    void testCallWithCredentials() throws Exception {
        mvn.useRepositoryCredentials([id: 'id', credentialsId: 'creds'])
        def result = mvn "test"
        assertEquals("test", result)

        assert 'creds' == scriptMock.actualUsernamePasswordArgs[0]['credentialsId']
        assert "NEXUS_REPO_CREDENTIALS_PASSWORD_0" == scriptMock.actualUsernamePasswordArgs[0]['passwordVariable']
        assert "NEXUS_REPO_CREDENTIALS_USERNAME_0" == scriptMock.actualUsernamePasswordArgs[0]['usernameVariable']

        assertSettingsXmlRepos('id')
    }

    @Test
    void testCallWithMultipleCredentials() throws Exception {
        mvn.useRepositoryCredentials([id: 'number0', credentialsId: 'creds0'],
                                      [id: 'number1', credentialsId: 'creds1'])
        def result = mvn "test"
        assertEquals("test", result)

        assert 'creds0' == scriptMock.actualUsernamePasswordArgs[0]['credentialsId']
        assert "NEXUS_REPO_CREDENTIALS_PASSWORD_0" == scriptMock.actualUsernamePasswordArgs[0]['passwordVariable']
        assert "NEXUS_REPO_CREDENTIALS_USERNAME_0" == scriptMock.actualUsernamePasswordArgs[0]['usernameVariable']

        assert 'creds1' == scriptMock.actualUsernamePasswordArgs[1]['credentialsId']
        assert "NEXUS_REPO_CREDENTIALS_PASSWORD_1" == scriptMock.actualUsernamePasswordArgs[1]['passwordVariable']
        assert "NEXUS_REPO_CREDENTIALS_USERNAME_1" == scriptMock.actualUsernamePasswordArgs[1]['usernameVariable']

        assertSettingsXmlRepos('number0', 'number1')
    }

    @Test
    void testGetVersion() {
        Maven mvn = new MavenForTest()
        assertEquals("Unexpected version returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout", mvn.getVersion())
    }
    
    @Test
    void testGetVersionWithCredentials() {
        mvn.useRepositoryCredentials([id: 'number0', credentialsId: 'creds0'])
        assertEquals("Unexpected version returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout", mvn.getVersion())
        
        assert 'creds0' == scriptMock.actualUsernamePasswordArgs[0]['credentialsId']
        assert "NEXUS_REPO_CREDENTIALS_PASSWORD_0" == scriptMock.actualUsernamePasswordArgs[0]['passwordVariable']
        assert "NEXUS_REPO_CREDENTIALS_USERNAME_0" == scriptMock.actualUsernamePasswordArgs[0]['usernameVariable']
    }

    @Test
    void testGetArtifactId() {
        Maven mvn = new MavenForTest()
        assertEquals("Unexpected artifact returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.artifactId -q -DforceStdout", mvn.getArtifactId())
    }

    @Test
    void testGetGroupId() {
        Maven mvn = new MavenForTest()
        assertEquals("Unexpected group returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.groupId -q -DforceStdout", mvn.getGroupId())
    }

    @Test
    void testGetName() {
        Maven mvn = new MavenForTest()
        assertEquals("Unexpected name returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.name -q -DforceStdout", mvn.getName())
    }

    @Test
    void testGetMavenProperty() {
        Maven mvn = new MavenForTest()
        assertEquals("Unexpected name returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=key -q -DforceStdout", mvn.getMavenProperty('key'))
    }

    @Test
    void testSetVersion() {
        Maven mvn = new MavenForTest()
        mvn.setVersion("2.1.0")
        assert mvnArgs.contains("versions:set -DgenerateBackupPoms=false -DnewVersion=2.1.0")
    }

    @Test
    void testSetVersionToNextMinorSnapshot() {
        Maven mvn = new MavenForTest()
        mvn.setVersionToNextMinorSnapshot()
        assert mvnArgs.contains("build-helper:parse-version versions:set -DgenerateBackupPoms=false -DnewVersion='\${parsedVersion.majorVersion}.\${parsedVersion.nextMinorVersion}.0-SNAPSHOT'")
    }

    @Test
    void testGetMavenPropertyWithMavenWrapper() {
        Maven mvn = new MavenWrapperForTest()
        assertEquals("Unexpected property returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=key -q -DforceStdout", mvn.getMavenProperty('key'))
    }

    @Test
    void testGetMavenPropertyWithMavenWrapperNotYetDownloaded() {
        Maven mvn = new MavenWrapperForTest()
        mvn.downloaded = false
        assertEquals("Unexpected property returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=key -q -DforceStdout", mvn.getMavenProperty('key'))
    }

    @Test
    void testDeployToNexusRepositoryNoRepository() {
        mvn.deployToNexusRepository()

        assert !mvnArgs.contains("-DaltReleaseDeploymentRepository")
        assert !mvnArgs.contains("-DaltSnapshotDeploymentRepository")
    }

    @Test
    void testUseRepositoryCredentialsMissingRequiredFieldsId() {
        assertMissingRepositoryParameter('id',
                { mvn.useRepositoryCredentials([url: 'url', credentialsId: 'creds']) } )
    }

    @Test
    void testUseRepositoryCredentialsMissingRequiredFields() {
        assertMissingRepositoryParameter('credentialsIdUsernameAndPassword',
                { mvn.useRepositoryCredentials([url: 'url', id: 'id']) } )
    }

    @Test
    void testDeployToNexusRepository() {
        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'expectedAdditionalArgs'
        deployToNexusRepository(DeployGoal.REGULAR, expectedAdditionalArgs, actualAdditionalArgs, 'deploy:deploy')
    }

    @Test
    void testDeployToNexusRepositoryWithMultipleCredentials() {
        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'expectedAdditionalArgs'
        deployToNexusRepository(actualAdditionalArgs, 'site:deploy',
                [[id: 'expectedId', credentialsId: 'expectedCredentials'],
                 [id: 'id', url: 'https://expected.url', credentialsId: 'creds', type: 'Nexus2']],
                { mvn.deploySiteToNexus(expectedAdditionalArgs) })
    }

    @Test
    void testDeployToNexusRepositoryWithMultipleUrls() {
        def exception = shouldFail {
            mvn.useRepositoryCredentials([id: 'id', credentialsId: 'creds', url: '1'],
                                         [id: '2', credentialsId: 'creds2', url: '2'])
        }

        assert "Multiple repositories with URL passed. Maven CLI only allows for passing one alt deployment repo." == exception.getMessage()
    }

    @Test
    void testDeployToNexus3Repository() {
        mvn.useRepositoryCredentials([id: 'id', url: 'https://expected.url', credentialsId: 'creds', type: 'Nexus3'])
        mvn.deployToNexusRepository()

        assert mvnArgs.contains("-DaltReleaseDeploymentRepository=id::default::https://expected.url/repository/maven-releases ")
        assert mvnArgs.contains("-DaltSnapshotDeploymentRepository=id::default::https://expected.url/repository/maven-snapshots ")
    }

    @Test
    void testDeployToNexusRepositoryWithSignature() {
        deployToNexusRepositoryWithSignature(DeployGoal.REGULAR, 'deploy:deploy', 'source:jar javadoc:jar package')
    }

    @Test
    void testDeployToNexusRepositoryWithStaging() {
        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'expectedAdditionalArgs'
        deployToNexusRepository(DeployGoal.NEXUS_STAGING, expectedAdditionalArgs, actualAdditionalArgs,
                expectedDeploymentGoalWithStaging, 'source:jar javadoc:jar package')
    }


    @Test
    void testDeployToNexus3RepositoryWithStaging() {
        mvn.useRepositoryCredentials([id: 'id', url: 'https://expected.url', credentialsId: 'creds', type: 'Nexus3'])
        mvn.deployToNexusRepositoryWithStaging()

        assert mvnArgs.contains("-DaltReleaseDeploymentRepository=id::default::https://expected.url/repository/maven-releases ")
        assert mvnArgs.contains("-DaltSnapshotDeploymentRepository=id::default::https://expected.url/repository/maven-snapshots ")
    }

    @Test
    void testDeployToNexusRepositoryWithStagingAndSignature() {
        deployToNexusRepositoryWithSignature(DeployGoal.NEXUS_STAGING, expectedDeploymentGoalWithStaging, 'source:jar javadoc:jar package')
    }

    @Test
    void testDeploySiteToNexusRepository() {
        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'expectedAdditionalArgs'
        deployToNexusRepository(actualAdditionalArgs, 'site:deploy',
                [[id: 'expectedId', credentialsId: 'expectedCredentials', type: 'Nexus2']],
                { mvn.deploySiteToNexus(expectedAdditionalArgs) })
    }

    @Test
    void testDeploySiteToNexusRepositoryWithoutUrl() {
        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'expectedAdditionalArgs'
        deployToNexusRepository(actualAdditionalArgs, 'site:deploy',
                [[id: 'expectedId', credentialsId: 'expectedCredentials', type: 'Nexus2']],
                { mvn.deploySiteToNexus(expectedAdditionalArgs) })
    }

    void deployToNexusRepositoryWithSignature(DeployGoal goal, String expectedDeploymentGoal, String beforeAdditionalArgs = '') {

        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'org.kohsuke:pgp-maven-plugin:sign expectedAdditionalArgs'

        scriptMock.env['ascFile'] = '/xyz/asc.file'
        scriptMock.env['passphrase'] = 'verySecret'

        mvn.setSignatureCredentials('expectedSecretKeyAscFile', 'expectedSecretKeyPassPhrase')
        deployToNexusRepository(goal, expectedAdditionalArgs, actualAdditionalArgs, expectedDeploymentGoal, beforeAdditionalArgs)

        assert 'expectedSecretKeyAscFile' == scriptMock.actualFileArgs['credentialsId']
        assert 'expectedSecretKeyPassPhrase' == scriptMock.actualStringArgs['credentialsId']

        assert scriptMock.actualWithEnv.size() == 2
        assert scriptMock.actualWithEnv.get(0) == 'PGP_SECRETKEY=keyfile:/xyz/asc.file'
        assert scriptMock.actualWithEnv.get(1) == 'PGP_PASSPHRASE=literal:verySecret'
    }

    private deployToNexusRepository(DeployGoal goal, String expectedAdditionalArgs, String actualAdditionalArgs,
                                    String expectedDeploymentGoal, String beforeAdditionalArgs = '') {
        deployToNexusRepository(actualAdditionalArgs, expectedDeploymentGoal,
                [[id  : 'expectedId', url: 'https://expected.url', credentialsId: 'expectedCredentials', type: 'Nexus2']],
                { mvn.deployToNexusRepository(goal, expectedAdditionalArgs)},
                beforeAdditionalArgs
        )
    }

    private deployToNexusRepository(String actualAdditionalArgs, String expectedDeploymentGoal, List<Map> repos,
                                    Closure methodUnderTest, String beforeAdditionalArgs = '') {
        def deploymentRepo = repos[0]
        for (Map repo : repos) {
            if (repo.url) {
                deploymentRepo = repo
            }
        }

        String deploymentRepoId = deploymentRepo.id
        def expectedCredentials = deploymentRepo.credentialsId
        def expectedUrl = deploymentRepo.url

        mvn.useRepositoryCredentials(repos.toArray(new Map[0]))
        methodUnderTest.call()

        def repoIds = []
        for (int i = 0; i < repos.size(); i++) {
            def repo = repos[i]

            assert repo.credentialsId == scriptMock.actualUsernamePasswordArgs[i]['credentialsId']
            assert "NEXUS_REPO_CREDENTIALS_PASSWORD_${i}" == scriptMock.actualUsernamePasswordArgs[i]['passwordVariable']
            assert "NEXUS_REPO_CREDENTIALS_USERNAME_${i}" == scriptMock.actualUsernamePasswordArgs[i]['usernameVariable']
            repoIds + repo.id
        }

        assertSettingsXmlRepos(repoIds.toArray(new String[0]))

        assert mvnArgs.startsWith('-DskipTests ')
        if (expectedUrl) {
            assert mvnArgs.contains("-DaltReleaseDeploymentRepository=${deploymentRepoId}::default::${expectedUrl}/content/repositories/releases ")
            assert mvnArgs.contains("-DaltSnapshotDeploymentRepository=${deploymentRepoId}::default::${expectedUrl}/content/repositories/snapshots ")
        } else {
            assert !mvnArgs.contains("-DaltReleaseDeploymentRepository")
            assert !mvnArgs.contains("-DaltSnapshotDeploymentRepository")
        }
        assert mvnArgs.contains('-s "/home/jenkins/workspaces/NAME/.m2/settings.xml" ')
        assert mvnArgs.endsWith("$beforeAdditionalArgs $actualAdditionalArgs $expectedDeploymentGoal")
    }

    private void assertSettingsXmlRepos(String... deploymentRepoIds) {
        assert scriptMock.writeFileParams.size() == 1
        def actualSettingsXml = scriptMock.writeFileParams.get(0)['text']
        
        for (int i = 0; i < deploymentRepoIds.size(); i++) {
            def deploymentRepoId = deploymentRepoIds[i]

            def string = "<server><id>${deploymentRepoId}</id>" +
                    "<username>\${env.NEXUS_REPO_CREDENTIALS_USERNAME_${i}}</username>" +
                    "<password>\${env.NEXUS_REPO_CREDENTIALS_PASSWORD_${i}}</password></server>"
            assert actualSettingsXml.contains(string)
        }
    }


    private static void assertMissingRepositoryParameter(String fieldKey, Closure methodUnderTest) {
        def exception = shouldFail {
            methodUnderTest.call()
        }

        assert "Missing required '${fieldKey}' parameter." == exception.getMessage()
    }

    class MavenForTest extends Maven {

        MavenForTest() {
            this(null)
        }

        MavenForTest(Object script) {
            super(script)
        }

        def mvn(String args, boolean printStdOut) {
            mvnArgs = args
            return args
        }
    }

    class MavenWrapperForTest extends Maven {

        private boolean downloaded = true

        MavenWrapperForTest() {
            this(null)
        }

        MavenWrapperForTest(Object script) {
            super(script)
        }

        def mvn(String args, boolean printStdOut) {
            // maven wrapper starts mostly with the current working directory
            String out = "/home/tricia/heartOfGold"
            if (!downloaded) {
                out += '\n--2020-03-11 15:07:29--  https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.5/maven-wrapper-0.5.5.jar'
                out += '\nResolving repo.maven.apache.org (repo.maven.apache.org)... 151.101.12.215'
                out += '\nMuch much more lines ...'
            }
            mvnArgs = args
            return out + '\n' + args
        }
    }
}
