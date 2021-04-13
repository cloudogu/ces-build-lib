package com.cloudogu.ces.cesbuildlib

class GradleMock extends Gradle {
    String args

    GradleMock(scriptMock) {
        super(scriptMock)
    }

    def gradle(String args, boolean printStdOut) {
        this.args = args
        return args
    }

    static Docker setupDockerMock(GradleInDockerBase gradle) {
        gradle.docker = DockerMock.create()
    }
}
