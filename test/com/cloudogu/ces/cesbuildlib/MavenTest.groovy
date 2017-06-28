package com.cloudogu.ces.cesbuildlib

import org.junit.After
import org.junit.Test

import static groovy.util.GroovyTestCase.assertEquals

class MavenTest {
    private static final String EOL = System.getProperty("line.separator");

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
        String expectedVersion = "1.0.0"
        def scripMock = [readFile: {
            "<project><groupId>com.cloudogu.ces</groupId><version>$expectedVersion</version></project>"
        }] as Object
        Maven mvn = new Maven(scripMock, null, null)
        assertEquals("Unexpected version returned", expectedVersion, mvn.getVersion())
    }

    @Test
    void testGetVersionMissing() {
        String expectedVersion = ""
        def scripMock = [readFile: { "<project><groupId>com.cloudogu.ces</groupId></project>" }] as Object
        Maven mvn = new Maven(scripMock, null, null)
        assertEquals("Unexpected version returned", expectedVersion, mvn.getVersion())
    }


    @Test
    void testGetMavenProperty() {
        String expectedPropertyKey = "expectedPropertyKey"
        String expectedPropertyValue = "expectedValue"
        def scripMock = [readFile: {
            "<project><groupId>com.cloudogu.ces</groupId><$expectedPropertyKey>NotInProperties!</$expectedPropertyKey><properties>" +
                    EOL +
                    "<dont>care</dont><$expectedPropertyKey>$expectedPropertyValue</$expectedPropertyKey>" +
                    EOL +
                    "</properties></project>"
        }] as Object
        Maven mvn = new Maven(scripMock, null, null)
        assertEquals("Unexpected version returned", expectedPropertyValue, mvn.getMavenProperty(expectedPropertyKey))
    }

    @Test
    void testGetMavenPropertyNoProperties() {
        String expectedPropertyKey = "expectedPropertyKey"
        String expectedPropertyValue = ""
        def scripMock = [readFile: { "<project><groupId>com.cloudogu.ces</groupId><$expectedPropertyKey>NotInProperties!</$expectedPropertyKey></project>" }] as Object
        Maven mvn = new Maven(scripMock, null, null)
        assertEquals("Unexpected version returned", expectedPropertyValue, mvn.getMavenProperty(expectedPropertyKey))
    }

    @Test
    void testGetMavenPropertyNoProperty() {
        String expectedPropertyKey = "expectedPropertyKey"
        String expectedPropertyValue = ""
        def scripMock = [readFile: { "<project><groupId>com.cloudogu.ces</groupId><$expectedPropertyKey>NotInProperties!</$expectedPropertyKey><properties><dont>care</dont></properties></project>" }] as Object
        Maven mvn = new Maven(scripMock, null, null)
        assertEquals("Unexpected version returned", expectedPropertyValue, mvn.getMavenProperty(expectedPropertyKey))
    }
}
