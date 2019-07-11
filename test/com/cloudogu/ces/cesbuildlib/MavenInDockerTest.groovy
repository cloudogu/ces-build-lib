package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static org.mockito.Mockito.verify
import static com.cloudogu.ces.cesbuildlib.MavenMock.setupDockerMock

class MavenInDockerTest {
    def scriptMock = new ScriptMock()

    @Test
    void mavenInDocker() {
        def mvn = new MavenInDocker(scriptMock, '3.5.0-jdk8')
        Docker docker = setupDockerMock(mvn)
        mvn 'clean install'

        assert scriptMock.actualShStringArgs[0].trim().endsWith('clean install')
        verify(docker).image('maven:3.5.0-jdk8')
    }
}