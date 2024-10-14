package com.cloudogu.ces.cesbuildlib

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyBoolean
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class DockerMock {
    Docker mock
    Docker.Image imageMock
    
    DockerMock(String imageTag = "") {
        mock = mock(Docker.class)
        imageMock = mock(Docker.Image.class)
        if (imageTag == "") {
            when(mock.image(anyString())).thenReturn(imageMock)
        } else {
            when(mock.image(imageTag)).thenReturn(imageMock)
        }
        when(imageMock.mountJenkinsUser()).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser(anyBoolean())).thenReturn(imageMock)
        when(imageMock.mountDockerSocket()).thenReturn(imageMock)
        when(imageMock.mountDockerSocket(anyBoolean())).thenReturn(imageMock)
        when(imageMock.inside(any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })
        when(mock.withRegistry(any(), any(), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                Closure closure = invocation.getArgument(2)
                closure.call()
            }
        })
    }

    static Docker create(String imageTag = "") {
        return new DockerMock(imageTag).mock
    }
}
