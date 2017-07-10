package com.cloudogu.ces.cesbuildlib
/**
 * Run maven in a docker container.
 * This can be helpful, when constants ports are bound during the build and concurrent builds cause port conflicts.
 * For example, when running integration tests.
 *
 * The builds base on the official maven containers from https://hub.docker.com/_/maven/
 */
class MavenInDocker extends Maven {

    String dockerImageVersion

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param dockerImageVersion the version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8}
     */
    MavenInDocker(script, String dockerImageVersion) {
        super(script)
        this.dockerImageVersion = dockerImageVersion
    }

    @Override
    def mvn(String args) {
        String jenkinsHome = script.env.HOME
        String commandLineArgs = createCommandLineArgs(args)

        writeDockerFile(dockerImageVersion)

        script.withEnv(["HOME=${script.pwd()}"]) {
            script.docker.build("ces-build-lib/maven/$dockerImageVersion", createDockerfilePath())
                    .inside("--volume=\"${jenkinsHome}/.m2/repository:${script.pwd()}/.m2/repository:rw\"") {
                script.sh "mvn ${commandLineArgs} -Duser.home=\"${script.pwd()}\""
            }
        }
    }

    /**
     * Write dockerfile, basing on desired image version and containing jenkins user
     */
    String writeDockerFile(String dockerImageVersion) {
        String dockerfile =
                "FROM maven:$dockerImageVersion\n" +
                "RUN echo \"${readJenkinsUserFromEtcPasswd()}\" >> /etc/passwd\n" +
                'USER JENKINS'

        def dockerfilePath = "${script.pwd()}/" + createDockerfilePath() + "/Dockerfile"
        script.writeFile file: dockerfilePath, text: "${dockerfile}"
        return dockerfilePath
    }

    String readJenkinsUserFromEtcPasswd() {
        // Query current Docker user string, e.g. "jenkins:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash"
        // An alternative (dirtier) approach: https://github.com/cloudogu/docker-golang/blob/master/Dockerfile
        String jenkinsUserFromEtcPasswd = script.sh(returnStdout: true,
                script: 'cat /etc/passwd | grep jenkins'
        ).trim()
        if (jenkinsUserFromEtcPasswd == null || jenkinsUserFromEtcPasswd.isEmpty()) {
            script.echo "WARN: Unable to parse user jenkins from /etc/passwd. Maven build will fail."
            // It would be wonderful if we could use exceptions in Jenkins Shared libraries...
        }
        jenkinsUserFromEtcPasswd
    }

    private String createDockerfilePath() {
        ".jenkins/build/$dockerImageVersion"
    }
}