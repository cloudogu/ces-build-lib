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
        assert scriptMock.actualShMapArgs.get(0) ==  'mkdir -p $HOME/.m2'
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

        def call(Closure closure) {
            inDocker(IMAGE_ID) {
                closure.call()
            }
        }
    }
}
