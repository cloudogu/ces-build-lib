package com.cloudogu.ces.cesbuildlib

import org.junit.Test

class ChangelogTest extends GroovyTestCase {

    @Test
    void testWillGetChangelogCorrect() {
        def mock = new ScriptMock()
        mock.expectedDefaultShRetValue = ""
        Changelog changelog = new Changelog("mychangelog", mock)
        changelog.get()

        assertEquals(1, mock.allActualArgs.size())
        assertEquals("cat mychangelog", mock.allActualArgs[0])
    }
}
