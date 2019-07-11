package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static com.cloudogu.ces.cesbuildlib.MavenMock.setupDockerMock
import static org.mockito.Mockito.verify

class MavenWrapperInDockerTest {
    def scriptMock = new ScriptMock()

    @Test
    void mavenInDocker() {
        def mvn = new MavenWrapperInDocker(scriptMock, 'adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine')
        Docker docker = setupDockerMock(mvn)
        mvn 'clean install'

        assert scriptMock.actualShStringArgs[0].trim().endsWith('clean install')
        verify(docker).image('adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine')
    }
}