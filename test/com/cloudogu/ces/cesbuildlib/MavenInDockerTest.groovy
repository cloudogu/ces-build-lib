package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.Test

import static org.mockito.Mockito.verify
import static com.cloudogu.ces.cesbuildlib.MavenMock.setupDockerMock

class MavenInDockerTest {
    def scriptMock = new ScriptMock()

    @Test
    void mavenInDocker() {
        def mvn = new MavenInDocker(scriptMock, '3.5.0-jdk8')
        Docker docker = setupDockerMock(mvn)
        mvn 'clean install'

        assert scriptMock.actualShStringArgs[0].trim().contains('clean install')
        verify(docker).image('maven:3.5.0-jdk8')
    }

    @Test
    void customMavenImageTest() {
        def mvn = new MavenInDocker(scriptMock, 'maven:latest')
        Docker docker = setupDockerMock(mvn)
        mvn 'clean install'

        verify(docker).image('maven:latest')
    }

}
