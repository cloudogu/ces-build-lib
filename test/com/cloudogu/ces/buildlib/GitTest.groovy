package com.cloudogu.ces.buildlib

import org.hamcrest.Matchers
import org.junit.Test
import static org.junit.Assert.*

class GitTest {

    @Test
    void testClean() throws Exception {
        String expectedParams = "params"
        def scriptMock = new ScriptMock()
        new Git(scriptMock).clean(expectedParams)
        assertThat(scriptMock.shArgs, Matchers.contains(
                "git clean -df --exclude $expectedParams".toString(),
                'git checkout -- .'))
    }

    @Test
    void testCleanEmpty() throws Exception {
        def scriptMock = new ScriptMock()
        new Git(scriptMock).clean("")
        assertThat(scriptMock.shArgs, Matchers.contains(
                "git clean -df",
                'git checkout -- .'))
    }

    static class ScriptMock {
        List<String> shArgs = new LinkedList<>()

        void sh(String args) {
            shArgs.add(args)
        }
    }
}
