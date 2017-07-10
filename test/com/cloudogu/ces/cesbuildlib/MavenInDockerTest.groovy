package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static junit.framework.TestCase.assertEquals
import static junit.framework.TestCase.assertTrue

class MavenInDockerTest {

    @Test
    void testWriteDockerFile() {
        def scriptMock = new ScriptMock()
        String expectedVersion = "3.5.0-jdk8"

        new MavenInDocker(scriptMock, expectedVersion).writeDockerFile(expectedVersion)

        assertTrue("script.writeFile() not called", scriptMock.getWriteFileParams().size() > 0)
        String actualDockerfile =  scriptMock.writeFileParams.get(0).get("text")
        assertTrue("Expected version $expectedVersion not contained in actual file: $actualDockerfile",
                actualDockerfile.contains(expectedVersion))
        String actualDockerfilePath =  scriptMock.writeFileParams.get(0).get("file")
        assertEquals("/.jenkins/build/Dockerfile", actualDockerfilePath)
    }

    class ScriptMock {
        List<Map<String, String>> writeFileParams = new LinkedList<>()

        String pwd() {
            ""
        }

        void writeFile(Map<String, String> params) {
            writeFileParams.add(params)
        }
    }
}
