package com.cloudogu.ces.cesbuildlib
/**
 * Run maven in a docker container.
 *
 * This can be helpful,
 * * when constant ports are bound during the build that cause port conflicts in concurrent builds.
 *   For example, when running integration tests, unit tests that use infrastructure that binds to ports or
 * * when one maven repo per builds is required
 *   For example when concurrent builds of multi module project install the same snapshot versions.
 *
 * The build are run inside the official maven containers from https://hub.docker.com/_/maven/
 */
class MavenInDocker extends Maven {

    /** The version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8} **/
    String dockerBaseImageVersion

    /** Setting this to {@code true} allows the maven build to access the docker host, i.e. to start other containers.*/
    boolean enableDockerHost = false

    Docker docker

    /**
     * @param script the Jenkinsfile instance ({@code this} in Jenkinsfile)
     * @param dockerBaseImageVersion the version of the maven docker image to use, e.g. {@code maven:3.5.0-jdk-8}
     */
    MavenInDocker(script, String dockerBaseImageVersion) {
        super(script)
        this.dockerBaseImageVersion = dockerBaseImageVersion
        this.docker = new Docker(script)
    }

    @Override
    def mvn(String args) {
        writeDockerFile()

        docker.build(createDockerImageName(), createDockerfilePath()).inside(createDockerRunArgs()) {
            script.sh("mvn ${createCommandLineArgs(args)}")
        }
    }

    String createDockerImageName() {
        // Encode the maven version and the workspace name in the image name,
        // because the image changes if any of those change.

        // e.g. /home/jenkins/workspace/NAME -> NAME
        def workspaceName = script.env.WORKSPACE.substring(script.env.WORKSPACE.lastIndexOf("/") + 1)
        // docker images must always be lower case
        "ces-build-lib/maven/${dockerBaseImageVersion}${workspaceName}".toLowerCase()
    }

    String createDockerRunArgs() {
        if (enableDockerHost) {
            "-v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=\"unix:///var/run/docker.sock\" --group-add ${readDockerGroupId()}"
        } else {
            ""
        }
    }

    /**
     * Write dockerfile, basing on desired image version and containing jenkins user
     */
    String writeDockerFile() {
        def dockerfilePath = "${script.pwd()}/" + createDockerfilePath() + "/Dockerfile"

        /* Add jenkins user. This is necessary for some commands such as npm.
           Add docker group. This is necessary to access Docker host from maven.*/
        script.writeFile file: dockerfilePath, text: """
            FROM maven:$dockerBaseImageVersion
            RUN echo \\"${readJenkinsUserFromEtcPasswd()}\\" >> /etc/passwd \
            && echo \\"${readDockerGroupFromEtcGroup()}\\" >> /etc/group"""
        return dockerfilePath
    }

    String readDockerGroupFromEtcGroup() {
        // Get the GID of the docker group, e.g. "docker:x:999:jenkins"
        def dockerGroupEtcGroup = script.sh(returnStdout: true,
                script: 'cat /etc/group | grep docker')
                .trim()
        if (dockerGroupEtcGroup == null || dockerGroupEtcGroup.isEmpty()) {
            script.echo "WARN: Unable to parse group docker from /etc/group. Docker host will not be accessible for maven."
        }
        dockerGroupEtcGroup
    }

    String readDockerGroupId() {
        // Get the GID of the docker group
        script.sh(returnStdout: true,
                script: "echo ${readDockerGroupFromEtcGroup()} | sed -E 's/.*:x:(.*):.*/\\1/'")
                .trim()
    }

    String readJenkinsUserFromEtcPasswd() {
        // Query current jenkins user string, e.g. "jenkins:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash"
        // An alternative (dirtier) approach: https://github.com/cloudogu/docker-golang/blob/master/Dockerfile
        String jenkinsUserFromEtcPasswd = script.sh(returnStdout: true,
                script: 'cat /etc/passwd | grep jenkins')
                .trim()
        if (jenkinsUserFromEtcPasswd == null || jenkinsUserFromEtcPasswd.isEmpty()) {
            script.echo "WARN: Unable to parse user jenkins from /etc/passwd. Maven build will fail."
            // It would be wonderful if we could use exceptions in Jenkins Shared libraries...
        }

        // java seems to read "user.home" frome here. So replace it.
        // This will cause maven and all forked JVMs to use the jenkins workspace as local repo
        // However, this will make this docker image specific to this workspace
        jenkinsUserFromEtcPasswd.replace("/home/jenkins", "${script.pwd()}")
    }

    private String createDockerfilePath() {
        ".jenkins/build/$dockerBaseImageVersion"
    }
}
