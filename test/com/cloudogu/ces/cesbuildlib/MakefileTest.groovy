package com.cloudogu.ces.cesbuildlib

import static org.assertj.core.api.Assertions.assertThat

class MakefileTest {

    void testGetVersion() {
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('grep -e "^VERSION=" Makefile | sed "s/VERSION=//g"'.toString(), "4.2.2".toString())

        Makefile makefile = new Makefile(scriptMock)

        makefile.getVersion()

        assertThat(scriptMock.allActualArgs[0]).isEqualTo('grep -e "^VERSION=" Makefile | sed "s/VERSION=//g"'.toString())
    }
}
