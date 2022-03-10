package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static org.mockito.Mockito.verify

class GradleWrapperInDockerTest {
    def scriptMock = new ScriptMock()

    @Test
    void gradleInDocker() {
        def gradle = new GradleWrapperInDocker(scriptMock, 'adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine')
        Docker docker = GradleMock.setupDockerMock(gradle)
        gradle 'clean install'

        assert scriptMock.actualShStringArgs[0].trim().contains('clean install')
        verify(docker).image('adoptopenjdk/openjdk11:jdk-11.0.1.13-alpine')
    }
}
