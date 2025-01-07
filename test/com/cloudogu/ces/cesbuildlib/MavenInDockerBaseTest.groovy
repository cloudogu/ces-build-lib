package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify 

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
    DockerMock docker = new DockerMock(IMAGE_ID)

    def mvn = new MavenInDockerForTest(scriptMock)

    @BeforeEach
    void setup() {
        mvn.docker = docker.mock
    }
    
    @Test
    void testCreateDockerRunArgsDefault() {
        assertEquals("", mvn.createDockerRunArgs())
    }

    @Test
    void testDockerHostEnabled() {
        mvn.enableDockerHost = true

        mvn 'test'

        verify(docker.imageMock).mountDockerSocket(true)
        verify(docker.imageMock).mountJenkinsUser(true)
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
    void inDockerWithRegistry() {

        mvn.credentialsId = 'myCreds'
        boolean closureCalled = false
        
        mvn.inDocker(IMAGE_ID, {
            closureCalled = true
        })
        
        assertThat(closureCalled).isTrue()
        verify(docker.mock).withRegistry(eq("https://$IMAGE_ID".toString()), eq('myCreds'), any())
    }

    class MavenInDockerScriptMock extends ScriptMock {
        MavenInDockerScriptMock() {
            expectedPwd = EXPECTED_PWD
        }

        @Override
        String sh(Map params) {
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

    class MavenInDockerForTest extends MavenInDockerBase {

        MavenInDockerForTest(Object script) {
            super(script)
        }

        def call(Closure closure, boolean printStdOut) {
            inDocker(IMAGE_ID) {
                closure.call()
            }
        }
    }
}
