package com.cloudogu.ces.cesbuildlib

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class MavenMock extends Maven {
    String args

    MavenMock(scriptMock) {
        super(scriptMock)
    }

    def mvn(String args) {
        this.args = args
    }

    static Docker setupDockerMock(MavenInDockerBase mvn) {
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image(anyString())).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser(anyBoolean())).thenReturn(imageMock)
        when(imageMock.mountDockerSocket(anyBoolean())).thenReturn(imageMock)
        when(imageMock.inside(anyString(), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })
        mvn.docker = dockerMock
    }
}