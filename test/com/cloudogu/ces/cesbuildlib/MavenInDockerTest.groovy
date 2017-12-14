package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static org.mockito.ArgumentMatchers.anyBoolean
import static org.mockito.Mockito.*

class MavenInDockerTest {
    static final ORIGINAL_USER_HOME = "/home/jenkins"
    static final String EXPECTED_JENKINS_USER_FROM_ETC_PASSWD =
            "jenkins:x:1000:1000:Jenkins,,,:" + ORIGINAL_USER_HOME + ":/bin/bash"
    static final EXPECTED_GROUP_ID = "999"
    static final EXPECTED_GROUP_FROM_ETC_GROUP = "docker:x:$EXPECTED_GROUP_ID:jenkins"
    // Expected output of pwd, print working directory
    static final EXPECTED_PWD = "/home/jenkins/workspaces/NAME"
    static final SOME_WHITESPACES = "  \n  "

    String expectedVersion = "3.5.0-jdk8"

    def scriptMock = new ScriptMock()

    def mavenInDocker = new MavenInDocker(scriptMock, expectedVersion)

    @Test
    void testCreateDockerRunArgsDefault() {
        assertEquals("", mavenInDocker.createDockerRunArgs())
    }

    @Test
    void testDockerHostEnabled() {
        mavenInDocker.enableDockerHost = true
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image('maven:3.5.0-jdk8')).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser(anyBoolean())).thenReturn(imageMock)
        when(imageMock.mountDockerSocket(anyBoolean())).thenReturn(imageMock)
        mavenInDocker.docker = dockerMock


        def expectedDockerRunArgs = "-v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=\"unix:///var/run/docker.sock\" --group-add " + EXPECTED_GROUP_ID

        mavenInDocker 'test'

        verify(imageMock).mountDockerSocket(true)
        verify(imageMock).mountJenkinsUser(true)
        //assertEquals(expectedDockerRunArgs, mavenInDocker.createDockerRunArgs())
    }

    @Test
    void testCreateDockerRunArgsUseLocalRepoFromJenkins() {
        scriptMock.env.HOME = "/home/jenkins"
        mavenInDocker.useLocalRepoFromJenkins = true


        def expectedMavenRunArgs = " -v /home/jenkins/.m2:$EXPECTED_PWD/.m2"

        assert mavenInDocker.createDockerRunArgs().contains(expectedMavenRunArgs)
        assert scriptMock.shParams.script ==  'mkdir -p $HOME/.m2'
    }

    class ScriptMock {
        def shParams
        List<Map<String, String>> writeFileParams = new LinkedList<>()

        String pwd() { EXPECTED_PWD }

        void writeFile(Map<String, String> params) {
            writeFileParams.add(params)
        }

        String sh(Map<String, String> params) {
            shParams = params
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

    void echo(String arg) {}

    def env = new Object() {
        String WORKSPACE = ""
        String HOME = ""
    }
}
