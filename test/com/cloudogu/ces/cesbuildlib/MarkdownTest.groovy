package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.Test

import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify

class MarkdownTest {

    @Test
    void testIfDockerContainerCommandIsCalledWithCorrectArgs() {
        Docker dockerMock = DockerMock.create("ghcr.io/tcort/markdown-link-check:stable")
        ScriptMock scriptMock = new ScriptMock(dockerMock)
        Markdown markdown = new Markdown(scriptMock)

        markdown.docker = dockerMock

        markdown.check()

        assert scriptMock.allActualArgs.size() == 1
        assert scriptMock.allActualArgs[0] == "find /docs -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v"

        verify(dockerMock.image("ghcr.io/tcort/markdown-link-check:stable"), times(1)).mountJenkinsUser()
    }

    @Test
    void testIfDockerContainerCommandIsCalledWithCorrectArgsWithMarkDownTag() {
        Docker dockerMock = DockerMock.create("ghcr.io/tcort/markdown-link-check:3.11.0")
        ScriptMock scriptMock = new ScriptMock(dockerMock)
        Markdown markdown = new Markdown(scriptMock, "3.11.0")

        markdown.docker = dockerMock

        markdown.check()

        assert scriptMock.allActualArgs.size() == 1
        assert scriptMock.allActualArgs[0] == "find /docs -name \\*.md -print0 | xargs -0 -n1 markdown-link-check -v"

        verify(dockerMock.image("ghcr.io/tcort/markdown-link-check:3.11.0"), times(1)).mountJenkinsUser()
    }
}
