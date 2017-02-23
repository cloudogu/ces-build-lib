package com.cloudogu.ces.buildlib

import org.junit.After
import org.junit.Test

import static groovy.util.GroovyTestCase.assertEquals

class MavenTest {

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        Maven.metaClass = null
    }

    @Test
    void testCall() throws Exception {
        Maven mvn = new Maven(null, null, null)
        mvn.metaClass.mvn = { String args ->
            return args
        }
        def result = mvn "test"
        assertEquals("test", result)
    }

    @Test
    void testGetVersion() {
        String expectedVersion="1.0.0"
        def scripMock= [readFile: { "<project><groupId>com.cloudogu.ces</groupId><version>$expectedVersion</version></project>" }] as Object
        Maven mvn = new Maven(scripMock, null, null)
        assertEquals("Unexpected version returned", expectedVersion, mvn.getVersion())
    }
}
