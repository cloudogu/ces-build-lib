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
}
