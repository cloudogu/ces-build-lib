package com.cloudogu.ces.cesbuildlib


import org.junit.After
import org.junit.Test

import static org.junit.Assert.assertEquals

class GradleTest {
    def scriptMock = new ScriptMock()
    def gradle = new GradleMock(scriptMock)

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        Gradle.metaClass = null
    }

    @Test
    void testCall() throws Exception {
        def result = gradle "test"
        assertEquals("test", result)
    }
}
