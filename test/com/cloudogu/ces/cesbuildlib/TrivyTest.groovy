package com.cloudogu.ces.cesbuildlib

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.matches
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class TrivyTest extends GroovyTestCase {

    void testScanImage_successfulTrivyExecution() {
        def scriptMock = new ScriptMock()
        scriptMock.env.WORKSPACE = "/test"
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image("aquasec/trivy:"+Trivy.DEFAULT_TRIVY_VERSION)).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser()).thenReturn(imageMock)
        when(imageMock.mountDockerSocket()).thenReturn(imageMock)
        when(imageMock.inside(matches("-v /test/.trivy/.cache:/root/.cache/"), any())).thenReturn(null)
        Trivy trivy = new Trivy(scriptMock, dockerMock)

        trivy.scanImage("nginx")
        // TODO: check that the build is not marked as unstable
    }

    void testScanImage_unsuccessfulTrivyExecution() {
        def scriptMock = new ScriptMock()
        scriptMock.env.WORKSPACE = "/test"
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image("aquasec/trivy:"+Trivy.DEFAULT_TRIVY_VERSION)).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser()).thenReturn(imageMock)
        when(imageMock.mountDockerSocket()).thenReturn(imageMock)
        when(imageMock.inside(matches("-v /test/.trivy/.cache:/root/.cache/"), any())).thenThrow(new RuntimeException("Trivy scan had errors: "))
        Trivy trivy = new Trivy(scriptMock, dockerMock)

        def exception = shouldFail {
            trivy.scanImage("inval!d:::///1.1...1.1.")
        }
        assert exception.contains("Trivy scan had errors: ")

        // TODO: check that the build is marked as failed
    }

    void testSaveFormattedTrivyReport() {
        notYetImplemented()
    }
}