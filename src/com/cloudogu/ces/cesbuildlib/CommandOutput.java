package com.cloudogu.ces.cesbuildlib;

public class CommandOutput {
    String stdout;
    int exitCode;

    public CommandOutput(String stdout, int exitCode) {
        this.stdout = stdout;
        this.exitCode = exitCode;
    }
}
