package com.cloudogu.ces.cesbuildlib

class Changelog {
    private String name
    private sh

    Changelog(String name, script) {
        this.name = name
        this.sh = new Sh(script)
    }

    String get() {
        return this.sh.returnStdOut("cat ${name}")
    }
}
