package com.cloudogu.ces.cesbuildlib

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.WaitStrategy
import org.testcontainers.utility.DockerImageName

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.matches
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class TrivyTest extends GroovyTestCase {

    void testScanImage_successfulTrivyExecution() {
        String imageName = "nginx"
        String severityLevel = TrivySeverityLevel.CRITICAL
        String strategy = TrivyScanStrategy.UNSTABLE
        String additionalFlags = "--db-repository public.ecr.aws/aquasecurity/trivy-db --java-db-repository public.ecr.aws/aquasecurity/trivy-java-db"
        String trivyReportFile = "trivy/trivyReport.json"
        String trivyImageCommand = "trivy image --exit-code 10 --exit-on-eol 10 --format ${TrivyScanFormat.JSON} -o ${trivyReportFile} --severity ${severityLevel} ${additionalFlags} ${imageName}"

        String trivyImage = "aquasec/trivy:" + Trivy.DEFAULT_TRIVY_VERSION
        def scriptMock = new ScriptMock()
        scriptMock.env.WORKSPACE = "/test"
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image(trivyImage)).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser()).thenReturn(imageMock)
        when(imageMock.mountDockerSocket()).thenReturn(imageMock)
        when(imageMock.inside(matches("-v /test/.trivy/.cache:/root/.cache/"), any())).thenAnswer(new Answer<Integer>() {
            @Override
            Integer answer(InvocationOnMock invocation) throws Throwable {
                Closure closure = invocation.getArgument(1)
                scriptMock.expectedShRetValueForScript.put(trivyImageCommand, 0)
                Integer statusCode = closure.call() as Integer
                assertEquals(0, statusCode)
                assertEquals(trivyImageCommand, scriptMock.getActualShMapArgs().getLast())

                def container = new GenericContainer(DockerImageName.parse(trivyImage))
                container.start()
                def result = container.execInContainer(trivyImageCommand)
                assertEquals(0, result.getExitCode())
                return (result as Integer).getExitCode()
            }
        })
        Trivy trivy = new Trivy(scriptMock, Trivy.DEFAULT_TRIVY_VERSION, dockerMock)

        trivy.scanImage(imageName)

        assertEquals(false, scriptMock.getUnstable())
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
        Trivy trivy = new Trivy(scriptMock, "0.57.1", dockerMock)

        def exception = shouldFail {
            trivy.scanImage("inval!d:::///1.1...1.1.")
        }
        assert exception.contains("Trivy scan had errors: ")
    }

    void testSaveFormattedTrivyReport() {
        notYetImplemented()
    }
}
