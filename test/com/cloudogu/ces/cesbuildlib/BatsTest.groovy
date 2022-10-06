package com.cloudogu.ces.cesbuildlib

import org.junit.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class BatsTest extends GroovyTestCase {

    ScriptMock scriptMock = new ScriptMock()

    @Test
    void test_Constructor() {
        // given
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.build(anyString(), anyString())).thenReturn(imageMock)

        // when
        Bats bats = new Bats(scriptMock, dockerMock)

        // then
        assertNotNull(bats)
    }

    @Test
    void test_checkAndExecuteTests() {
        // given
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.build("cloudogu/bats:1.2.1", "--build-arg=BATS_BASE_IMAGE=bats/bats --build-arg=BATS_TAG=1.2.1 ./build/make/bats")).thenReturn(imageMock)
        when(imageMock.inside(anyString(), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })

        Bats bats = new Bats(scriptMock, dockerMock)

        // when
        bats.checkAndExecuteTests()

        // then
        assertThat(scriptMock.actualEcho[0].trim()).contains("Executing bats tests with config:")
        assertThat(scriptMock.actualEcho[1].trim()).contains("[bats_base_image:bats/bats, bats_custom_image:cloudogu/bats, bats_tag:1.2.1]")

        verify(dockerMock).build("cloudogu/bats:1.2.1", "--build-arg=BATS_BASE_IMAGE=bats/bats --build-arg=BATS_TAG=1.2.1 ./build/make/bats")
        verify(imageMock).inside(eq("--entrypoint='' -v :/workspace -v /testdir:/usr/share/webapps"), any())

        assertEquals("true", scriptMock.actualJUnitFlags["allowEmptyResults"].toString())
        assertEquals("target/shell_test_reports/*.xml", scriptMock.actualJUnitFlags["testResults"].toString())
    }

    @Test
    void test_checkAndExecuteTests_with_custom_config() {
        // given
        def defaultSetupConfig = [
            bats_custom_image: "myimage/bats",
            bats_tag         : "1.4.1"
        ]

        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.build("myimage/bats:1.4.1", "--build-arg=BATS_BASE_IMAGE=bats/bats --build-arg=BATS_TAG=1.4.1 ./build/make/bats")).thenReturn(imageMock)
        when(imageMock.inside(anyString(), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })

        Bats bats = new Bats(scriptMock, dockerMock)

        // when
        bats.checkAndExecuteTests(defaultSetupConfig)

        // then
        assertThat(scriptMock.actualEcho[0].trim()).contains("Executing bats tests with config:")
        assertThat(scriptMock.actualEcho[1].trim()).contains("[bats_base_image:bats/bats, bats_custom_image:myimage/bats, bats_tag:1.4.1]")

        verify(dockerMock).build("myimage/bats:1.4.1", "--build-arg=BATS_BASE_IMAGE=bats/bats --build-arg=BATS_TAG=1.4.1 ./build/make/bats")
        verify(imageMock).inside(eq("--entrypoint='' -v :/workspace -v /testdir:/usr/share/webapps"), any())

        assertEquals("true", scriptMock.actualJUnitFlags["allowEmptyResults"].toString())
        assertEquals("target/shell_test_reports/*.xml", scriptMock.actualJUnitFlags["testResults"].toString())
    }
}
