package com.cloudogu.ces.cesbuildlib

import org.junit.Test

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

        def result = makefile.getDevelopBranchName()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("4.2.2/develop")
    }

    @Test
    void testIsBackportMainBranchName() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "4.2.2".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.getMainBranchName()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("4.2.2/main")
    }

    @Test
    void testIsStandardDevelopBranchName() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.getDevelopBranchName()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("develop")
    }

    @Test
    void testIsStandardMainBranchName() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString(), "".toString())

        Makefile makefile = new Makefile(scriptMock)

        def result = makefile.getMainBranchName()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"'.toString())
        assertThat(result).isEqualTo("main")
    }
}
