package com.cloudogu.ces.cesbuildlib

class CommandOutput {
    String stdout
    int exitCode

    CommandOutput(String stdout, int exitCode) {
        this.stdout = stdout
        this.exitCode = exitCode
    }
}
