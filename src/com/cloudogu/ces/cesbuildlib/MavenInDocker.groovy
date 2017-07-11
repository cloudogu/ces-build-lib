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

    String dockerImageVersion
    /** Setting this to {@code true} allows the maven build to access the docker host, i.e. to start other containers.*/
    boolean enableDockerHost = false

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
        writeSettingsXml()
        writeDockerFile()

        // Jenkins docker plugin automatically mounts current workspace as working dir of container
        // So, set it consistently as env var and system property
        script.withEnv(["HOME=${script.pwd()}"]) {
            script.docker.build("ces-build-lib/maven/$dockerImageVersion", createDockerfilePath())
                    .inside(createDockerRunArgs()) {
                script.sh "mvn -Duser.home=\"${script.pwd()}\" -s \"${script.pwd()}/.m2/settings.xml\"  " +
                        "${createCommandLineArgs(args)}"
            }
        }
    }

    String createDockerRunArgs() {
        if (enableDockerHost) {
            "-v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=\"unix:///var/run/docker.sock\" --group-add ${readDockerGroupId()}"
        } else {
            ""
        }
    }

    /**
     * Create settings.xml in workspace, pointing to .m2 repo ins workspace
     */
    void writeSettingsXml() {
        script.writeFile file: "${script.pwd()}/.m2/settings.xml", text: """
            <settings>
                <localRepository>${script.pwd()}/.m2/repository</localRepository>
            </settings>"""
    }

    /**
     * Write dockerfile, basing on desired image version and containing jenkins user
     */
    String writeDockerFile() {
        def dockerfilePath = "${script.pwd()}/" + createDockerfilePath() + "/Dockerfile"

        /* Add jenkins user. This is necessary for some commands such as npm.
           Add docker group. This is necessary to access Docker host from maven.*/
        script.writeFile file: dockerfilePath, text:  """
            FROM maven:$dockerImageVersion
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
        jenkinsUserFromEtcPasswd
    }

    private String createDockerfilePath() {
        ".jenkins/build/$dockerImageVersion"
    }
}