package com.cloudogu.ces.cesbuildlib

class Makefile {
    private script
    private Sh sh

    /**
     * Creates an object to conveniently read from a Makefile in the current directory.
     *
     * @param script The Jenkins script you are coming from (aka "this")
     */
    Makefile(script) {
        this.script = script
        this.sh = new Sh(script)
    }

    /**
     * Retrieves the value of the VERSION Variable defined in the Makefile.
     */
    String getVersion() {
        return sh.returnStdOut('grep -e "^VERSION=" Makefile | sed "s/VERSION=//g"')
    }

    /**
     * Retrieves the value of the BASE_VERSION Variable defined in the Makefile.
     */
    String getBaseVersion() {
        return sh.returnStdOut('grep -e "^BASE_VERSION=" Makefile | sed "s/BASE_VERSION=//g"')
    }

    /**
     * Retrieves the value of the BASE_VERSION Variable defined in the Makefile.
     */
    String getDevelopBranchName() {
        def develop = "develop"
        def baseVersion = getBaseVersion()
        if (baseVersion != null && baseVersion != "") {
            return baseVersion + "/" + develop
        }
        return develop
    }

    /**
     * Retrieves the value of the BASE_VERSION Variable defined in the Makefile.
     */
    String getMainBranchName() {
        def main = "main"
        def baseVersion = getBaseVersion()
        if (baseVersion != null && baseVersion != "") {
            return baseVersion + "/" + main
        }
        return main
    }
}
