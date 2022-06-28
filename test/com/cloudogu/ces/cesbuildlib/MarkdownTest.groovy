package com.cloudogu.ces.cesbuildlib

import org.junit.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.mockito.Mockito.when


class MarkdownTest extends GroovyTestCase {
    @Test
    void testIfDockerIsCalledWithCorrectArgs() {
        Docker dockerMock = DockerMock.create()
        ScriptMock scriptMock = new ScriptMock(dockerMock)
        Markdown markdown = new Markdown(scriptMock)

        markdown.docker = dockerMock

        markdown.check()
        when(dockerMock.image("ghcr.io/tcort/markdown-link-check:stable")).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                scriptMock.sh("docker image call")
            }
        })
        assert scriptMock.allActualArgs.size() == 8
        assert scriptMock.allActualArgs[0] == "docker build call"
    }
}
