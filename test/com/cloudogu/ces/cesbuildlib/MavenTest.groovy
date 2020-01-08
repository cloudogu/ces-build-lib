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
        Maven mvn = new MavenForTest()
        mvn.metaClass.mvn = { String args ->
            return args
        }
        def result = mvn "test"
        assertEquals("test", result)
    }

    @Test
    void testGetVersion() {
        Maven mvn = new MavenForTest()
        assertEquals("Unexpected version returned",
                "org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout", mvn.getVersion())
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
    void testDeployToNexusRepositoryNoRepository() {
        def exception = shouldFail {
            mvn.deployToNexusRepository()
        }

        assert 'No deployment repository set. Cannot perform maven deploy.' == exception.getMessage()
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsRegularId() {
        mvn.useRepositoryCredentials([url: 'url', credentialsId: 'creds'])
        assertMissingRepositoryParameter('id',
                { mvn.deployToNexusRepository() })
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsRegularUrl() {
        mvn.useRepositoryCredentials([id: 'id', credentialsId: 'creds'])
        assertMissingRepositoryParameter('url',
                { mvn.deployToNexusRepository() })
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsRegularCredentials() {
        mvn.useRepositoryCredentials([id: 'id', url: 'url'])
        assertMissingRepositoryParameter('credentialsIdUsernameAndPassword',
                { mvn.deployToNexusRepository() })
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsStagingId() {
        mvn.useRepositoryCredentials([url: 'url', credentialsId: 'creds'])
        assertMissingRepositoryParameter('id',
                { mvn.deployToNexusRepositoryWithStaging() })
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsStagingUrl() {
        mvn.useRepositoryCredentials([id: 'id', credentialsId: 'creds'])
        assertMissingRepositoryParameter('url',
                { mvn.deployToNexusRepositoryWithStaging() })
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsStagingCredentials() {
        mvn.useRepositoryCredentials([id: 'id', url: 'url'])
        assertMissingRepositoryParameter('credentialsIdUsernameAndPassword',
                { mvn.deployToNexusRepositoryWithStaging() })
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsSiteId() {
        mvn.useRepositoryCredentials([url: 'url', credentialsId: 'creds'])
        assertMissingRepositoryParameter('id',
                { mvn.deploySiteToNexus() })
    }

    @Test
    void testDeployToNexusRepositoryMissingRequiredFieldsSiteCredentials() {
        mvn.useRepositoryCredentials([id: 'id', url: 'url'])
        assertMissingRepositoryParameter('credentialsIdUsernameAndPassword',
                { mvn.deploySiteToNexus() })
    }

    @Test
    void testDeployToNexusRepository() {
        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'expectedAdditionalArgs'
        deployToNexusRepository(DeployGoal.REGULAR, expectedAdditionalArgs, actualAdditionalArgs, 'deploy:deploy')
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
                [id: 'expectedId', credentialsId: 'expectedCredentials',  url: 'https://expected.url', type: 'Nexus2'],
                { mvn.deploySiteToNexus(expectedAdditionalArgs) })
    }

    @Test
    void testDeploySiteToNexusRepositoryWithoutUrl() {
        def expectedAdditionalArgs = 'expectedAdditionalArgs'
        def actualAdditionalArgs = 'expectedAdditionalArgs'
        deployToNexusRepository(actualAdditionalArgs, 'site:deploy',
                [id: 'expectedId', credentialsId: 'expectedCredentials', type: 'Nexus2'],
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
                [id  : 'expectedId', url: 'https://expected.url', credentialsId: 'expectedCredentials',
                 type: 'Nexus2'],
                { mvn.deployToNexusRepository(goal, expectedAdditionalArgs)},
                beforeAdditionalArgs
        )
    }

    private deployToNexusRepository(String actualAdditionalArgs, String expectedDeploymentGoal, Map deploymentRepo,
                                    Closure methodUnderTest, String beforeAdditionalArgs = '') {
        String deploymentRepoId = deploymentRepo.id
        def expectedCredentials = deploymentRepo.credentialsId
        def expectedUrl = deploymentRepo.url

        mvn.useRepositoryCredentials(deploymentRepo)
        methodUnderTest.call()

        assert expectedCredentials == scriptMock.actualUsernamePasswordArgs['credentialsId']
        assert "NEXUS_REPO_CREDENTIALS_PASSWORD" == scriptMock.actualUsernamePasswordArgs['passwordVariable']
        assert "NEXUS_REPO_CREDENTIALS_USERNAME" == scriptMock.actualUsernamePasswordArgs['usernameVariable']

        assert scriptMock.writeFileParams.size() == 1
        def actualSettingsXml = scriptMock.writeFileParams.get(0)['text']
        assert actualSettingsXml.contains("<id>${deploymentRepoId}</id>")
        assert actualSettingsXml.contains("<username>\${env.NEXUS_REPO_CREDENTIALS_USERNAME}</username>")
        assert actualSettingsXml.contains("<password>\${env.NEXUS_REPO_CREDENTIALS_PASSWORD}</password>")

        assert mvnArgs.startsWith('-DskipTests ')
        assert mvnArgs.contains("-DaltReleaseDeploymentRepository=${deploymentRepoId}::default::${expectedUrl}/content/repositories/releases ")
        assert mvnArgs.contains("-DaltSnapshotDeploymentRepository=${deploymentRepoId}::default::${expectedUrl}/content/repositories/snapshots ")
        assert mvnArgs.contains('-s "/home/jenkins/workspaces/NAME/.m2/settings.xml" ')
        assert mvnArgs.endsWith("$beforeAdditionalArgs $actualAdditionalArgs $expectedDeploymentGoal")
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
}
