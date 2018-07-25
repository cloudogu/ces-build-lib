package com.cloudogu.ces.cesbuildlib

class MavenMock extends Maven {
    String args

    MavenMock(scriptMock) {
        super(scriptMock)
    }

    def mvn(String args) {
        this.args = args
    }
}