package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class MavenWrapperTest {

    def scriptMock = new ScriptMock()
    def mvn = new MavenWrapper(scriptMock)


    @Test
    void testCall() throws Exception {
        mvn 'ourGoal'
        assert scriptMock.getActualShStringArgs().size() == 1
        assert scriptMock.getActualShStringArgs().get(0).startsWith('./mvnw')
        assert scriptMock.getActualShStringArgs().get(0).contains('ourGoal')
    }
}
