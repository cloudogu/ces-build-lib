package com.cloudogu.ces.cesbuildlib

class TrivyTest extends GroovyTestCase {

    void testScanImage_invalidImageName() {
        def scriptMock = new ScriptMock()
        scriptMock.env.WORKSPACE = "."
        Trivy trivy = new Trivy(scriptMock)

        int result = trivy.scanImage("invalid///:::1.1.!!.1.1")

        assertNotSame(0, result)
        assertNotSame(1, result)
    }

    void testSaveFormattedTrivyReport() {
    }
}
