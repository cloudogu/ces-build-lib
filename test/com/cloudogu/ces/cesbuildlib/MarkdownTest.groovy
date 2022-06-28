package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify

class MarkdownTest extends GroovyTestCase {
    @Test
    void testIfDockerContainerCommandIsCalledWithCorrectArgs() {
        Docker dockerMock = DockerMock.create()
        ScriptMock scriptMock = new ScriptMock(dockerMock)
        Markdown markdown = new Markdown(scriptMock)

        markdown.docker = dockerMock

        markdown.check()

        assert scriptMock.allActualArgs.size() == 1
        assert scriptMock.allActualArgs[0] == "find /tmp -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v"

        verify(dockerMock.image(""), times(1)).mountJenkinsUser()
    }
}
