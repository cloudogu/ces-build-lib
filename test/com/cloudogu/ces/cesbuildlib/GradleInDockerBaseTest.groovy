package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.verify

class GradleInDockerBaseTest {


    static final IMAGE_ID = 'adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine'
    def scriptMock = new GradleWrapperInDockerBaseScriptMock()
    DockerMock docker = new DockerMock(IMAGE_ID)

    static final ORIGINAL_USER_HOME = "/home/jenkins"
    static final String EXPECTED_JENKINS_USER_FROM_ETC_PASSWD =
        "jenkins:x:1000:1000:Jenkins,,,:" + ORIGINAL_USER_HOME + ":/bin/bash"
    static final EXPECTED_GROUP_ID = "999"
    static final EXPECTED_GROUP_FROM_ETC_GROUP = "docker:x:$EXPECTED_GROUP_ID:jenkins"
    // Expected output of pwd, print working directory
    static final EXPECTED_PWD = "/home/jenkins/workspaces/NAME"
    static final SOME_WHITESPACES = "  \n  "

    @Test
    void inDockerWithRegistry() {
        def gradle = new GradleInDockerBaseForTest(scriptMock, 'myCreds')
        gradle.docker = docker.mock
        scriptMock.expectedDefaultShRetValue = ''
        boolean closureCalled = false

        gradle.inDocker(IMAGE_ID, {
            closureCalled = true
        })

        assertThat(closureCalled).isTrue()
        verify(docker.mock).withRegistry(eq("https://$IMAGE_ID".toString()), eq('myCreds'), any())
    }

    class GradleWrapperInDockerBaseScriptMock extends ScriptMock {
        GradleWrapperInDockerBaseScriptMock() {
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

    class GradleInDockerBaseForTest extends GradleInDockerBase {

        GradleInDockerBaseForTest(script, String credentialsId = null) {
            super(script, credentialsId)
        }

        def call(Closure closure, boolean printStdOut) {
            inDocker(IMAGE_ID) {
                closure.call()
            }
        }
    }

}
