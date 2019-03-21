package com.cloudogu.ces.cesbuildlib


import org.junit.Test

class MavenLocalTest {

    def scriptMock = new ScriptMock()


    @Test
    void testMvnHomeNull() throws Exception {
        def mvn = new MavenLocal(scriptMock, null, "javaHome")
        mvn "test"

        assert scriptMock.actualEcho[0] == 'WARNING: mvnHome is empty. Did you check "Install automatically"?'
    }

    @Test
    void testMvnHomeEmpty() throws Exception {
        def mvn = new MavenLocal(scriptMock, '', "javaHome")
        mvn "test"

        assert scriptMock.actualEcho[0] == 'WARNING: mvnHome is empty. Did you check "Install automatically"?'
    }

    @Test
    void testJavaHomeNull() throws Exception {
        def mvn = new MavenLocal(scriptMock, 'mvnHome', null)
        mvn "test"

        assert scriptMock.actualEcho[0] == 'WARNING: javaHome is empty. Did you check "Install automatically"?'
    }

    @Test
    void testJavaHomeEmpty() throws Exception {
        def mvn = new MavenLocal(scriptMock, 'mvnHome', '')
        mvn "test"

        assert scriptMock.actualEcho[0] == 'WARNING: javaHome is empty. Did you check "Install automatically"?'
    }
}
