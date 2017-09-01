package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertTrue

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
    void testWriteDockerFile() {

        mavenInDocker.writeDockerFile()

        assertTrue("script.writeFile() not called", scriptMock.getWriteFileParams().size() > 0)
        String actualDockerfile = scriptMock.writeFileParams.get(0).get("text")
        assertTrue("Expected version $expectedVersion not contained in actual dockerfile: $actualDockerfile",
                actualDockerfile.contains(expectedVersion))
        def expected_jenkins_user = EXPECTED_JENKINS_USER_FROM_ETC_PASSWD.replace(ORIGINAL_USER_HOME, EXPECTED_PWD)
        assertTrue("Expected user \"${expected_jenkins_user}\" not contained in actual dockerfile: $actualDockerfile",
                actualDockerfile.contains(expected_jenkins_user))
        String actualDockerfilePath = scriptMock.writeFileParams.get(0).get("file")
        assertEquals("$EXPECTED_PWD/.jenkins/build/$expectedVersion/Dockerfile", actualDockerfilePath)
    }

    @Test
    void testCreateDockerImageName() {
        String workspaceName = "NAME";
        scriptMock.env.WORKSPACE = "/home/jenkins/workspace/$workspaceName"
        def actualImageName = mavenInDocker.createDockerImageName()
        def expectedImageName = "ces-build-lib/maven/${expectedVersion}${workspaceName.toLowerCase()}"

        assertEquals(expectedImageName, actualImageName)
    }

    @Test
    void testCreateDockerRunArgsNoDockerHost() {
        assertEquals("", mavenInDocker.createDockerRunArgs())
    }

    @Test
    void testCreateDockerRunArgsDockerHostEnabled() {
        mavenInDocker.enableDockerHost = true

        def expectedDockerRunArgs = "-v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=\"unix:///var/run/docker.sock\" --group-add " + EXPECTED_GROUP_ID

        assertEquals(expectedDockerRunArgs, mavenInDocker.createDockerRunArgs())
    }

    class ScriptMock {
        List<Map<String, String>> writeFileParams = new LinkedList<>()

        String pwd() { EXPECTED_PWD }

        void writeFile(Map<String, String> params) {
            writeFileParams.add(params)
        }

        String sh(Map<String, String> params) {
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
    }
}
