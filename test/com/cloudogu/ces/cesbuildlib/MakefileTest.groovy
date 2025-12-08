package com.cloudogu.ces.cesbuildlib

import org.junit.Test
import org.junit.jupiter.api.DisplayName

import static org.assertj.core.api.Assertions.assertThat

class MakefileTest {

    @Test
    void testGetVersion() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^VERSION=" Makefile | sed "s/VERSION=//g"'.toString(), "4.2.2".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.getVersion()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^VERSION=" Makefile | sed "s/VERSION=//g"'.toString())
        assertThat(result).isEqualTo("4.2.2")
    }

    @Test
    void testGetBaseVersion() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "4.2.2".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.getBaseVersion()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("4.2.2")
    }

    @Test
    void testIsBackportDevelopBranchName() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "4.2.2".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.determineGitFlowDevelopBranch()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("4.2.2/develop")
    }

    @Test
    void testIsBackportMainBranchName() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "4.2.2".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.determineGitFlowMainBranch()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("4.2.2/main")
    }

    @Test
    void testIsStandardDevelopBranchName() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.determineGitFlowDevelopBranch()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("develop")
    }

    @Test
    void testIsStandardMainBranchName() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.determineGitFlowMainBranch()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("main")
    }

    @Test
    @DisplayName("should replace master with main if BASE_VERSION is used and the default branch is master")
    void shouldReplaceMasterWithMain() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "4.5".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.determineGitFlowMainBranch("master")

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("4.5/main")
    }

    @Test
    @DisplayName("should use default branch if BASE_VERSION is not used")
    void shouldUseDefaultBranch() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.determineGitFlowMainBranch("master")

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("master")
    }
}
