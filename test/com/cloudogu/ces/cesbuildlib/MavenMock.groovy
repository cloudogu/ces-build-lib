package com.cloudogu.ces.cesbuildlib 

class MavenMock extends Maven {
    String args
    String mockedGroupId = ""
    String mockedArtifactId = ""
    String mockedName = ""

    MavenMock(scriptMock) {
        super(scriptMock)
    }

    def mvn(String args, boolean printStdOut) {
        this.args = args
    }

    static Docker setupDockerMock(MavenInDockerBase mvn) {
        mvn.docker = DockerMock.create()
    }

    @Override
    String getArtifactId() { mockedArtifactId }

    @Override
    String getGroupId() { mockedGroupId }

    @Override
    String getName() { mockedName }

}
