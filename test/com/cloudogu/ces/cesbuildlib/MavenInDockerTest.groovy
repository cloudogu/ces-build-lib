package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertTrue

class MavenInDockerTest {
    public static final String EXPECTED_JENKINS_USER_FROM_ETC_PASSWD =
            "jenkins:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash"
    public static final String EXPECTED_DOCKER_HOST = "172.17.0.1"

    String expectedVersion = "3.5.0-jdk8"

    def scriptMock = new ScriptMock()

    def mavenInDocker = new MavenInDocker(scriptMock, expectedVersion)

    @Test
    void testWriteDockerFile() {

        mavenInDocker.writeDockerFile()

        assertTrue("script.writeFile() not called", scriptMock.getWriteFileParams().size() > 0)
        String actualDockerfile =  scriptMock.writeFileParams.get(0).get("text")
        assertTrue("Expected version $expectedVersion not contained in actual dockerfile: $actualDockerfile",
                actualDockerfile.contains(expectedVersion))
        assertTrue("Expected user $EXPECTED_JENKINS_USER_FROM_ETC_PASSWD not contained in actual dockerfile: $actualDockerfile",
                actualDockerfile.contains(EXPECTED_JENKINS_USER_FROM_ETC_PASSWD))
        String actualDockerfilePath =  scriptMock.writeFileParams.get(0).get("file")
        assertEquals("/.jenkins/build/$expectedVersion/Dockerfile", actualDockerfilePath)
    }

    class ScriptMock {
        List<Map<String, String>> writeFileParams = new LinkedList<>()

        String pwd() {
            ""
        }

        void writeFile(Map<String, String> params) {
            writeFileParams.add(params)
        }

        String sh(Map<String, String> params) {
            // Add some whitespaces
            return EXPECTED_JENKINS_USER_FROM_ETC_PASSWD + "  \n  "
        }

        void echo (String arg) {}
    }
}
