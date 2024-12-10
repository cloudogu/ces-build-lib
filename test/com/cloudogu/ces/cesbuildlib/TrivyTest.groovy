package com.cloudogu.ces.cesbuildlib

import junit.framework.AssertionFailedError
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.matches
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class TrivyTest extends GroovyTestCase {

    String additionalFlags = "--db-repository public.ecr.aws/aquasecurity/trivy-db --java-db-repository public.ecr.aws/aquasecurity/trivy-java-db"
    Path installDir = Paths.get("target/trivyInstalls")
    Path workDir = Paths.get("")
    TrivyExecutor trivyExec = new TrivyExecutor(installDir)
    String trivyImage = "aquasec/trivy:" + Trivy.DEFAULT_TRIVY_VERSION


    ScriptMock doTestScan(String imageName, String severityLevel, String strategy, int expectedStatusCode) {
        File trivyReportFile = new File("trivy/trivyReport.json")
        Path trivyDir = Paths.get(trivyReportFile.getParent())
        String trivyArguments = "image --exit-code 10 --exit-on-eol 10 --format ${TrivyScanFormat.JSON} -o ${trivyReportFile} --severity ${severityLevel} ${additionalFlags} ${imageName}"
        String expectedTrivyCommand = "trivy $trivyArguments"

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
                // mock "sh trivy" so that it returns the expected status code and check trivy arguments
                Closure closure = invocation.getArgument(1)
                scriptMock.expectedShRetValueForScript.put(expectedTrivyCommand, expectedStatusCode)
                Integer statusCode = closure.call() as Integer
                assertEquals(expectedStatusCode, statusCode)
                assertEquals(expectedTrivyCommand, scriptMock.getActualShMapArgs().getLast())

                // emulate trivy call with local trivy installation and check that it has the same behavior
                Files.createDirectories(trivyDir)
                Process process = trivyExec.exec(Trivy.DEFAULT_TRIVY_VERSION, trivyArguments, workDir)
                if(process.waitFor(2, TimeUnit.MINUTES)) {
                    assertEquals(expectedStatusCode, process.exitValue())
                } else {
                    process.destroyForcibly()
                    fail("terminate trivy due to timeout")
                }

                return expectedStatusCode
            }
        })
        Trivy trivy = new Trivy(scriptMock, Trivy.DEFAULT_TRIVY_VERSION, Trivy.DEFAULT_TRIVY_IMAGE, dockerMock)

        trivy.scanImage(imageName, severityLevel, strategy)

        return scriptMock
    }

    void testScanImage_successfulTrivyExecution() {
        // with hopes that this image will never have CVEs
        String imageName = "hello-world"
        String severityLevel = TrivySeverityLevel.CRITICAL

        def scriptMock = doTestScan(imageName, severityLevel, TrivyScanStrategy.UNSTABLE, 0)

        assertEquals(false, scriptMock.getUnstable())
    }

    void testScanImage_unstableBecauseOfCVEs() {
        // with hopes that this image will always have CVEs
        String imageName = "alpine:3.18.7"
        String severityLevel = TrivySeverityLevel.ALL

        def scriptMock = doTestScan(imageName, severityLevel, TrivyScanStrategy.UNSTABLE, 10)

        assertEquals(true, scriptMock.getUnstable())
    }

    void testScanImage_failBecauseOfCVEs() {
        // with hopes that this image will always have CVEs
        String imageName = "alpine:3.18.7"
        String severityLevel = TrivySeverityLevel.ALL

        def gotException = false
        try {
            doTestScan(imageName, severityLevel, TrivyScanStrategy.FAIL, 10)doTestScan(imageName, severityLevel, TrivyScanStrategy.FAIL, 10)
        } catch (AssertionFailedError e) {
            // exception could also be a junit assertion exception. This means a previous assertion failed
            throw e
        } catch (Exception e) {
            assertTrue("exception is: ${e.getMessage()}", e.getMessage().contains("Trivy has found vulnerabilities in image"))
            gotException = true
        }
        assertTrue(gotException)
    }

    void testScanImage_unsuccessfulTrivyExecution() {
        // with hopes that this image will always have CVEs
        String imageName = "inval!d:::///1.1...1.1."
        String severityLevel = TrivySeverityLevel.ALL

        def gotException = false
        try {
            doTestScan(imageName, severityLevel, TrivyScanStrategy.FAIL, 1)
        } catch (AssertionFailedError e) {
            // exception could also be a junit assertion exception. This means a previous assertion failed
            throw e
        } catch (Exception e) {
            assertTrue("exception is: ${e.getMessage()}", e.getMessage().contains("Error during trivy scan; exit code: 1"))
            gotException = true
        }
        assertTrue(gotException)
    }
}
