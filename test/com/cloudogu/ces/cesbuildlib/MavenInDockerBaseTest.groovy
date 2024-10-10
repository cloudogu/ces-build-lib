package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static org.mockito.ArgumentMatchers.anyBoolean
import static org.mockito.Mockito.*

class MavenInDockerBaseTest {

    static final ORIGINAL_USER_HOME = "/home/jenkins"
    static final String EXPECTED_JENKINS_USER_FROM_ETC_PASSWD =
        "jenkins:x:1000:1000:Jenkins,,,:" + ORIGINAL_USER_HOME + ":/bin/bash"
    static final EXPECTED_GROUP_ID = "999"
    static final EXPECTED_GROUP_FROM_ETC_GROUP = "docker:x:$EXPECTED_GROUP_ID:jenkins"
    // Expected output of pwd, print working directory
    static final EXPECTED_PWD = "/home/jenkins/workspaces/NAME"
    static final SOME_WHITESPACES = "  \n  "
    static final IMAGE_ID = 'maven:3.5.0-jdk8'

    def scriptMock = new MavenInDockerScriptMock()

    def mvn = new MavenInDockerTest(scriptMock)

    @Test
    void testCreateDockerRunArgsDefault() {
        assertEquals("", mvn.createDockerRunArgs())
    }

    @Test
    void testDockerHostEnabled() {
        mvn.enableDockerHost = true
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image(IMAGE_ID)).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser(anyBoolean())).thenReturn(imageMock)
        when(imageMock.mountDockerSocket(anyBoolean())).thenReturn(imageMock)
        mvn.docker = dockerMock

        mvn 'test'

        verify(imageMock).mountDockerSocket(true)
        verify(imageMock).mountJenkinsUser(true)
    }

    @Test
    void testCreateDockerRunArgsUseLocalRepoFromJenkins() {
        scriptMock.env.HOME = "/home/jenkins"
        mvn.useLocalRepoFromJenkins = true

        def expectedMavenRunArgs = " -v /home/jenkins/.m2:$EXPECTED_PWD/.m2"

        assert mvn.createDockerRunArgs().contains(expectedMavenRunArgs)
        assert scriptMock.actualShMapArgs.size() == 1
        assert scriptMock.actualShMapArgs.get(0) == 'mkdir -p $HOME/.m2'
    }

    @Test
    void testDockerImageParsingFromName() {

        def result = Docker.parseRegistryImageNameAndTagFromString('maven:latest')
        assertEquals(result.registry, "docker.io")
        assertEquals(result.imageName, "maven")
        assertEquals(result.tag, "latest")

        def result1 = Docker.parseRegistryImageNameAndTagFromString('gcr.io/maven:latest')
        assertEquals(result1.registry, "gcr.io")
        assertEquals(result1.imageName, "maven")
        assertEquals(result1.tag, "latest")

        def result2 = Docker.parseRegistryImageNameAndTagFromString('gcr.io/cytopia/yamllint:latest')
        assertEquals(result2.registry, "gcr.io")
        assertEquals(result2.imageName, "cytopia/yamllint")
        assertEquals(result2.tag, "latest")

        def result3 = Docker.parseRegistryImageNameAndTagFromString('cytopia/yamllint:latest')
        assertEquals(result3.registry, "docker.io")
        assertEquals(result3.imageName, "cytopia/yamllint")
        assertEquals(result3.tag, "latest")

        def result4 = Docker.parseRegistryImageNameAndTagFromString('cytopia/yamllint:v13.393.33')
        assertEquals(result4.registry, "docker.io")
        assertEquals(result4.imageName, "cytopia/yamllint")
        assertEquals(result4.tag, "v13.393.33")

        def result5 = Docker.parseRegistryImageNameAndTagFromString('eu.gcr.io/cytopia/yamllint:v13.393.33')
        assertEquals(result5.registry, "eu.gcr.io")
        assertEquals(result5.imageName, "cytopia/yamllint")
        assertEquals(result5.tag, "v13.393.33")

        def result6 = Docker.parseRegistryImageNameAndTagFromString('eu.gcr.io/maven:v13.393.33')
        assertEquals(result6.registry, "eu.gcr.io")
        assertEquals(result6.imageName, "maven")
        assertEquals(result6.tag, "v13.393.33")

        def result7 = Docker.parseRegistryImageNameAndTagFromString('eu.gcr.io/maven:latest')
        assertEquals(result7.registry, "eu.gcr.io")
        assertEquals(result7.imageName, "maven")
        assertEquals(result7.tag, "latest")

        def result8 = Docker.parseRegistryImageNameAndTagFromString('eu.gcr.io/maven:latest')
        assert !(result8.registry == "gcr.io")
        assert !(result8.imageName == "mavenn")
        assert !(result8.tag == "latestt")
    }

    class MavenInDockerScriptMock extends ScriptMock {
        MavenInDockerScriptMock() {
            expectedPwd = EXPECTED_PWD
        }

        @Override
        String sh(Map<String, String> params) {
            super.sh(params)
            // Add some whitespaces
            String script = params.get("script")
            if (script == "cat /etc/passwd | grep jenkins") {
                return EXPECTED_JENKINS_USER_FROM_ETC_PASSWD + SOME_WHITESPACES
            } else if (script == "cat /etc/group | grep docker") {
                return EXPECTED_GROUP_FROM_ETC_GROUP + SOME_WHITESPACES
            } else if (script.contains(EXPECTED_GROUP_FROM_ETC_GROUP)) {
                return EXPECTED_GROUP_ID
            }
            ""
        }
    }

    class MavenInDockerTest extends MavenInDockerBase {

        MavenInDockerTest(Object script) {
            super(script)
        }

        def call(Closure closure, boolean printStdOut) {
            inDocker(IMAGE_ID) {
                closure.call()
            }
        }
    }
}
