package com.cloudogu.ces.cesbuildlib

import org.hamcrest.Matchers
import org.junit.After
import org.junit.Test

import static groovy.util.GroovyTestCase.assertEquals
import static org.junit.Assert.*

class GitTest {

    @After
    void tearDown() throws Exception {
        // always reset metaClass after messing with it to prevent changes from leaking to other tests
        Git.metaClass = null
    }

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

    @Test
    void testCall() throws Exception {
        Git git = new Git(new ScriptMock())
        git.metaClass.git = { String args ->
            return args
        }
        def result = git "https://repoUrl"
        assertEquals("https://repoUrl", result)
    }

    static class ScriptMock {
        List<String> shArgs = new LinkedList<>()

        void sh(String args) {
            shArgs.add(args)
        }

        def git(def args) {
            return args;
        }
    }
}
