package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class MavenWrapperTest {

    def scriptMock = new ScriptMock()



    @Test
    void testCallWithoutJavaHome() throws Exception {
        def mvn = new MavenWrapper(scriptMock)
        mvn 'ourGoal'
        assert scriptMock.getActualShStringArgs().size() == 1
        assert scriptMock.getActualShStringArgs().get(0).startsWith('./mvnw')
        assert scriptMock.getActualShStringArgs().get(0).contains('ourGoal')
        assert scriptMock.actualWithEnv == null
    }

    @Test
    void testCallWithJavaHome() throws Exception {
        def mvn = new MavenWrapper(scriptMock, '/java')
        scriptMock.env.JAVA_HOME = '/env/java'
        mvn 'ourGoal'

        assert scriptMock.getActualShStringArgs().size() == 1
        assert scriptMock.getActualShStringArgs().get(0).startsWith('./mvnw')
        assert scriptMock.getActualShStringArgs().get(0).contains('ourGoal')
        assert scriptMock.actualWithEnv == ["JAVA_HOME=/java", "PATH+JDK=/env/java/bin"]
    }
}
