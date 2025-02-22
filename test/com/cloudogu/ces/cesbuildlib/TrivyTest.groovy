package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.opentest4j.AssertionFailedError

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import static org.junit.jupiter.api.Assertions.*
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.matches
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class TrivyTest {

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
                if (process.waitFor(2, TimeUnit.MINUTES)) {
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

    @Test
    void testScanImage_successfulTrivyExecution() {
        // with hopes that this image will never have CVEs
        String imageName = "hello-world"
        String severityLevel = TrivySeverityLevel.CRITICAL

        def scriptMock = doTestScan(imageName, severityLevel, TrivyScanStrategy.UNSTABLE, 0)

        assertEquals(false, scriptMock.getUnstable())
    }

    @Test
    void testScanImage_unstableBecauseOfCVEs() {
        // with hopes that this image will always have CVEs
        String imageName = "alpine:3.18.7"
        String severityLevel = TrivySeverityLevel.ALL

        def scriptMock = doTestScan(imageName, severityLevel, TrivyScanStrategy.UNSTABLE, 10)

        assertEquals(true, scriptMock.getUnstable())
    }

    @Test
    void testScanImage_failBecauseOfCVEs() {
        // with hopes that this image will always have CVEs
        String imageName = "alpine:3.18.7"
        String severityLevel = TrivySeverityLevel.ALL

        def gotException = false
        try {
            doTestScan(imageName, severityLevel, TrivyScanStrategy.FAIL, 10)
        } catch (AssertionFailedError e) {
            // exception could also be a junit assertion exception. This means a previous assertion failed
            throw e
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Trivy has found vulnerabilities in image"), "exception is: ${e.getMessage()}")
            gotException = true
        }
        assertTrue(gotException)
    }

    @Test
    void testScanImage_unsuccessfulTrivyExecution() {
        String imageName = "inval!d:::///1.1...1.1."
        String severityLevel = TrivySeverityLevel.ALL

        def gotException = false
        try {
            doTestScan(imageName, severityLevel, TrivyScanStrategy.FAIL, 1)
        } catch (AssertionFailedError e) {
            // exception could also be a junit assertion exception. This means a previous assertion failed
            throw e
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Error during trivy scan; exit code: 1"), "exception is: ${e.getMessage()}")
            gotException = true
        }
        assertTrue(gotException)
    }

    @Test
    void testSaveFormattedTrivyReport_HtmlAllSeverities() {
        Trivy trivy = mockTrivy(
            "template --template \"@/contrib/html.tpl\"",
            "UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL",
            "trivy/formattedTrivyReport.html")
        trivy.saveFormattedTrivyReport()
    }

    @Test
    void testSaveFormattedTrivyReport_JsonCriticalSeverity() {
        Trivy trivy = mockTrivy(
            "json",
            "CRITICAL",
            "trivy/formattedTrivyReport.json")
        trivy.saveFormattedTrivyReport(TrivyScanFormat.JSON, TrivySeverityLevel.CRITICAL)
    }

    @Test
    void testSaveFormattedTrivyReport_TableHighAndUpSeverity() {
        Trivy trivy = mockTrivy(
            "table",
            "CRITICAL,HIGH",
            "trivy/formattedTrivyReport.table")
        trivy.saveFormattedTrivyReport(TrivyScanFormat.TABLE, TrivySeverityLevel.HIGH_AND_ABOVE)
    }

    @Test
    void testSaveFormattedTrivyReport_MediumAndUpSeverity() {
        Trivy trivy = mockTrivy(
            "sarif",
            "CRITICAL,HIGH,MEDIUM",
            "trivy/formattedTrivyReport.txt")
        trivy.saveFormattedTrivyReport("sarif", TrivySeverityLevel.MEDIUM_AND_ABOVE)
    }

    @Test
    void testSaveFormattedTrivyReport_CustomFilename() {
        Trivy trivy = mockTrivy(
            "json",
            "CRITICAL,HIGH,MEDIUM",
            "trivy/myOutput.custom")
        trivy.saveFormattedTrivyReport(TrivyScanFormat.JSON, TrivySeverityLevel.MEDIUM_AND_ABOVE, "myOutput.custom")
    }

    @Test
    void testSaveFormattedTrivyReport_UnsupportedFormat() {
        def scriptMock = new ScriptMock()
        scriptMock.env.WORKSPACE = "/test"
        Trivy trivy = new Trivy(scriptMock, Trivy.DEFAULT_TRIVY_VERSION, Trivy.DEFAULT_TRIVY_IMAGE, mock(Docker.class))
        def gotException = false
        try {
            trivy.saveFormattedTrivyReport("UnsupportedFormat", TrivySeverityLevel.MEDIUM_AND_ABOVE)
        } catch (AssertionFailedError e) {
            // exception could also be a junit assertion exception. This means a previous assertion failed
            throw e
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("This format did not match the supported formats"), "exception is: ${e.getMessage()}")
            gotException = true
        }
        assertTrue(gotException)
    }

    @Test
    void testSaveFormattedTrivyReport_UnsupportedSeverity() {
        def scriptMock = new ScriptMock()
        scriptMock.env.WORKSPACE = "/test"
        Trivy trivy = new Trivy(scriptMock, Trivy.DEFAULT_TRIVY_VERSION, Trivy.DEFAULT_TRIVY_IMAGE, mock(Docker.class))
        def gotException = false
        try {
            trivy.saveFormattedTrivyReport(TrivyScanFormat.JSON, "UnsupportedSeverity")
        } catch (AssertionFailedError e) {
            // exception could also be a junit assertion exception. This means a previous assertion failed
            throw e
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("The severity levels provided (UnsupportedSeverity) do not match the " +
                "applicable levels (UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL)."), "exception is: ${e.getMessage()}")
            gotException = true
        }
        assertTrue(gotException)
    }

    Trivy mockTrivy(String expectedFormat, String expectedSeverity, String expectedOutput) {
        String trivyArguments = "convert --format ${expectedFormat} --severity ${expectedSeverity} --output ${expectedOutput} trivy/trivyReport.json"
        String expectedTrivyCommand = "trivy $trivyArguments"

        def scriptMock = new ScriptMock()
        scriptMock.env.WORKSPACE = "/test"
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.image(trivyImage)).thenReturn(imageMock)
        when(imageMock.inside(matches("-v /test/.trivy/.cache:/root/.cache/"), any())).thenAnswer(new Answer<Integer>() {
            @Override
            Integer answer(InvocationOnMock invocation) throws Throwable {
                // mock "sh trivy" so that it returns the expected status code and check trivy arguments
                Closure closure = invocation.getArgument(1)
                scriptMock.expectedShRetValueForScript.put(expectedTrivyCommand, 0)
                closure.call()
                assertEquals(expectedTrivyCommand, scriptMock.getActualShMapArgs().getLast())
                return 0
            }
        })
        Trivy trivy = new Trivy(scriptMock, Trivy.DEFAULT_TRIVY_VERSION, Trivy.DEFAULT_TRIVY_IMAGE, dockerMock)
        return trivy
    }
}
