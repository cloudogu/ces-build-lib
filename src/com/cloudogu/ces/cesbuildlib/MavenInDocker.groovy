package com.cloudogu.ces.cesbuildlib

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

/**
 * Run maven in a docker container.
 * This can be helpful, when constants ports are bound during the build and concurrent builds cause port conflicts.
 * For example, when running integration tests.
 *
 * The builds base on the official maven containers from https://hub.docker.com/_/maven/
 */
class MavenInDocker extends Maven {
    private static final DOCKERFILE_TEMPLATE =
            'FROM maven:$dockerImageVersion\n' +
            'RUN echo \"jenkins:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash\" >> /etc/passwd\n' +
            'USER JENKINS'

    String dockerImageVersion

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param dockerImageVersion the version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8}
     */
    MavenInDocker(script, String dockerImageVersion) {
        super(script, null, null)
        this.dockerImageVersion = dockerImageVersion
    }

    @Override
    def mvn(String args) {
        String jenkinsHome = script.env.HOME
        String commandLineArgs = createCommandLineArgs(args)

        writeDockerFile(dockerImageVersion)

        script.withEnv(["HOME=${script.pwd()}"]) {
            script.docker.image("maven:$dockerImageVersion")
                    .inside("--volume=\"${jenkinsHome}/.m2/repository:${script.pwd()}/.m2/repository:rw\"") {
                script.sh "mvn ${commandLineArgs} -Duser.home=\"${script.pwd()}\" -s \"${script.pwd()}/.m2/settings.xml\""
            }
        }
    }

    def writeDockerFile(String dockerImageVersion) {
        Template template = new SimpleTemplateEngine().createTemplate(DOCKERFILE_TEMPLATE)
        def variableBindingsForTemplate = ['dockerImageVersion' : dockerImageVersion]
        Writable dockerfile = template.make(variableBindingsForTemplate)
        def dockerfilePath = "${script.pwd()}/.jenkins/build/Dockerfile"
        script.writeFile file: dockerfilePath, text: "${dockerfile.toString()}"
        return dockerfilePath
    }
}