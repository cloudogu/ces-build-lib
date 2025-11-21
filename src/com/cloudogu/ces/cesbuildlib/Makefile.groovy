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
     * Creates the develop branch for Git Flow based on the base version.
     */
    String getGitFlowDevelopBranch() {
        def develop = "develop"
        def baseVersion = getBaseVersion()
        if (baseVersion != null && baseVersion != "") {
            return baseVersion + "/" + develop
        }
        return develop
    }

    /**
     * Creates the main branch for Git Flow based on the base version.
     */
    String getGitFlowMainBranch(defaultBranch="main") {
        def baseVersion = getBaseVersion()
        if (baseVersion != null && baseVersion != "") {
            // The master branch is legacy so we don't create one here, even if it was passed as parameter.
            return baseVersion + "/main"
        }
        return defaultBranch
    }
}
