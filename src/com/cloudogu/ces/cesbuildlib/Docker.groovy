package com.cloudogu.ces.cesbuildlib
/**
 * Basic abstraction for docker.
 *
 * Provides all methods of the global docker-variable (see https://<jenkinsUlr>/job/<jobname>/pipeline-syntax/globals#docker)
 * as well as some convenience methods.
 */
class Docker implements Serializable {
    def script
    Sh sh

    Docker(script) {
        this.script = script
        this.sh = new Sh(script)
    }

    /**
     * @param container docker container instance
     * @return the IP address for a docker container instance
     */
    String findIp(container) {
        sh.returnStdOut "docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${container.id}"
    }

    /**
     * @param container docker container instance
     * @return the environment variables set within the docker container as string
     */
    String findEnv(container) {
        sh.returnStdOut "docker exec ${container.id} env"
    }

    /**
     * @param container docker container instance
     * @return {@code true} if the container is in state running, otherwise {@code false}
     */
    boolean isRunning(container) {
        return Boolean.valueOf(sh.returnStdOut("docker inspect -f {{.State.Running}} ${container.id}"))
    }

    /* Define methods of global docker variable again.
       Using methodMissing(String name, args) is not possible, because we would need the spread operator for dynamic
       arguments (*) which is not supported by Jenkins Pipeline

        def methodMissing(String name, args) {
            return this.script.docker."$name"(*args)
        }

       Leads to UnsupportedOperationException.

       Instead ust delegate all methods manually. See
       https://github.com/jenkinsci/docker-workflow-plugin/blob/master/src/main/resources/org/jenkinsci/plugins/docker/workflow/Docker.groovy
     */

    /**
     * Specifies a registry URL such as https://docker.mycorp.com/, plus an optional credentials ID to connect to it.
     *
     * Example:
     * <pre>
     *   def dockerImage = docker.build("image/name:1.0", "folderOfDockfile")
     *   docker.withRegistry("https://your.registry", 'credentialsId') {*       dockerimage().push()
     *}*  </pre>
     */
    def withRegistry(String url, String credentialsId = null, Closure body) {
        return this.script.docker.withRegistry(url, credentialsId, body)
    }

    /**
     * Specifies a server URI such as tcp://swarm.mycorp.com:2376, plus an optional credentials ID to connect to it.
     */
    def withServer(String uri, String credentialsId = null, Closure body) {
        return this.script.docker.withServer(uri, credentialsId, body)
    }

    /**
     * Specifies the name of a Docker installation to use, if any are defined in Jenkins global configuration.
     * If unspecified, docker is assumed to be in the $PATH of the Jenkins agent.
     */
    def withTool(String toolName, Closure body) {
        return this.script.docker.withTool(toolName, body)
    }

    /**
     * Creates an Image object with a specified name or ID.
     *
     * Example:
     * <pre>
     *      docker.image('google/cloud-sdk:164.0.0').inside("-e HOME=${pwd()}") {*          sh "echo something"
     *}*  </pre>
     */
    def image(String id) {
        return new Image(this.script, id)
    }

    /**
     * Runs docker build to create and tag the specified image from a Dockerfile in the current directory.
     * Additional args may be added, such as '-f Dockerfile.other --pull --build-arg http_proxy=http://192.168.1.1:3128 .'.
     * Like docker build, args must end with the build context.
     *
     * Example:
     * <pre>
     *   def dockerContainer = docker.build("image/name:1.0", "folderOfDockfile").run("-e HOME=${pwd()}")
     *  </pre>
     *
     * @return the resulting Image object. Records a FROM fingerprint in the build
     */
    def build(String image, String args = '.') {
        return this.script.docker.build(image, args)
    }

    static class Image implements Serializable {

        private String DOCKER_CLIENT_PATH = ".jenkins/docker"

        private final script

        // The wrapped image object
        private image

        // Don't mix this up with Jenkins docker.image.id, which is an ImageNameTokens object
        // See getId()
        private String imageIdString

        Sh sh

        private boolean mountJenkinsUser = false

        private boolean mountDockerSocket = false

        private String dockerClientVersionToInstall = ""

        Image(script, String id) {
            imageIdString = id
            this.script = script
            this.sh = new Sh(script)
        }

        // Creates an image instance. Can't be called from constructor because of CpsCallableInvocation
        // See https://issues.jenkins-ci.org/browse/JENKINS-26313
        private def image() {
            if (!image) {
                image = script.docker.image(imageIdString)
            }
            return image
        }

        String imageName() {
            return image().imageName()
        }

        /** The image name with optional tag (mycorp/myapp, mycorp/myapp:latest) or ID (hexadecimal hash).  **/
        String getId() {
            return image().id
        }

        /**
         * Like withRun this starts a container for the duration of the body, but all external commands (sh) launched
         * by the body run inside the container rather than on the host. These commands run in the same working
         * directory (normally a Jenkins agent workspace), which means that the Docker server must be on localhost.
         */
        def inside(String args = '', Closure body) {
            def extendedArgs = extendArgs(args)
            return image().inside(extendedArgs, body)
        }

        /**
         * Runs docker pull. Not necessary before run, withRun, or inside.
         */
        void pull() {
            script.docker.pull()
        }

        /**
         *  Uses docker run to run the image, and returns a Container which you could stop later. Additional args may
         *  be added, such as '-p 8080:8080 --memory-swap=-1'. Optional command is equivalent to Docker command
         *  specified after the image(). Records a run fingerprint in the build.
         */
        def run(String args = '', String command = "") {
            def extendedArgs = extendArgs(args)
            return image().run(extendedArgs, command)
        }

        /**
         * Like run but stops the container as soon as its body exits, so you do not need a try-finally block.
         */
        def withRun(String args = '', String command = "", Closure body) {
            def extendedArgs = extendArgs(args)
            return image().withRun(extendedArgs, command, body)
        }

        /**
         * Runs docker tag to record a tag of this image (defaulting to the tag it already has). Will rewrite an
         * existing tag if one exists.
         */
        void tag(String tagName = image().parsedId.tag, boolean force = true) {
            image().tag(tagName, force)
        }

        /**
         * Pushes an image to the registry after tagging it as with the tag method. For example, you can use image().push
         * 'latest' to publish it as the latest version in its repository.
         */
        void push(String tagName = image().parsedId.tag, boolean force = true) {
            image().push(tagName, force)
        }

        /**
         * Provides the user that executes the build within docker container's /etc/passwd.
         * This is necessary for some commands such as npm, ansible, git, id, etc. Those might exit with errors without
         * a user present.
         *
         * Why?
         * Note that Jenkins starts Docker containers in the pipeline with the -u parameter (e.g. -u 1042:1043).
         * That is, the container does not run as root (which is a good thing from a security point of view).
         * However, the userID/UID (e.g. 1042) and the groupID/GID (e.g. 1043) will most likely not be present within the
         * container which causes errors in some executables.
         *
         * How?
         * Setting this will cause the creation of a {@code passwd} file that is mounted into a container started
         * from this image() (triggered by run(), withRun() and inside() methods).
         * This {@code passwd} file contains the username, UID, GID of the user that executes the build and also sets
         * the current workspace as HOME within the docker container.
         */
        Image mountJenkinsUser(boolean mountJenkinsUser = true) {
            this.mountJenkinsUser = mountJenkinsUser
            return this
        }

        /** Setting this to {@code true} mounts the docker socket into the container.
         * This allows the container to start other containers "next to" itself.
         * Note that this is similar but not the same as "Docker In Docker". */
        Image mountDockerSocket(boolean mountDockerSocket = true) {
            this.mountDockerSocket = mountDockerSocket
            return this
        }

        /** Installs the docker client with the specified version inside the container.
         * This can be called in addition to mountDockerSocket(), when the "docker" CLI is required on the PATH.
         *
         *  For available versions see here: https://download.docker.com/linux/static/stable/x86_64/
         */
        Image installDockerClient(String version) {
            this.dockerClientVersionToInstall = version
            return this
        }

        private extendArgs(String args) {
            String extendedArgs = args
            if (mountJenkinsUser) {
                String passwdPath = writePasswd()
                extendedArgs += " -v ${script.pwd()}/${passwdPath}:/etc/passwd:ro "
            }
            if (mountDockerSocket) {
                String groupPath = writeGroup()
                extendedArgs +=
                        // Mount the docker socket
                        " -v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=\"unix:///var/run/docker.sock\" " +
                                // Mount the docker group
                                "-v ${script.pwd()}/${groupPath}:/etc/group:ro --group-add ${readDockerGroupId()} "
            }
            if (!dockerClientVersionToInstall.isEmpty()) {
                doInstallDockerClient()
                extendedArgs += " -v ${script.pwd()}/${DOCKER_CLIENT_PATH}/docker:/usr/bin/docker"
            }
            return extendedArgs
        }

        private String writePasswd() {
            def passwdPath = '.jenkins/etc/passwd'

            // e.g. "jenkins:x:1000:1000::/home/jenkins:/bin/sh"
            String passwd = readJenkinsUserFromEtcPasswdCutOffAfterGroupId() + ":${script.pwd()}:/bin/sh"

            script.writeFile file: passwdPath, text: passwd
            return passwdPath
        }

        private String writeGroup() {
            def groupPath = '.jenkins/etc/group'

            // e.g. "docker:x:999:jenkins"
            String group = readDockerGroupFromEtcGroup()

            script.writeFile file: groupPath, text: group
            return groupPath
        }

        /**
         * Return from /etc/passwd (for user that executes build) only username, pw, UID and GID.
         * e.g. "jenkins:x:1000:1000:"
         */
        private String readJenkinsUserFromEtcPasswdCutOffAfterGroupId() {
            def regexMatchesUntilFourthColon = '(.*?:){4}'

            def etcPasswd = readJenkinsUserFromEtcPasswd()

            // Storing matcher in a variable might lead to java.io.NotSerializableException: java.util.regex.Matcher
            if (!(etcPasswd =~ regexMatchesUntilFourthColon)) {
                script.error '/etc/passwd entry for current user does not match user:x:uid:gid:'
            }
            return (etcPasswd =~ regexMatchesUntilFourthColon)[0][0]
        }

        private String readJenkinsUserFromEtcPasswd() {
            // Query current jenkins user string, e.g. "jenkins:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash"
            // An alternative (dirtier) approach: https://github.com/cloudogu/docker-golang/blob/master/Dockerfile
            def userName = sh.returnStdOut('whoami')
            String jenkinsUserFromEtcPasswd = sh.returnStdOut "cat /etc/passwd | grep $userName"

            if (jenkinsUserFromEtcPasswd.isEmpty()) {
                script.error 'Unable to parse user jenkins from /etc/passwd.'
            }
            return jenkinsUserFromEtcPasswd
        }

        private String readDockerGroupFromEtcGroup() {
            // Get the GID of the docker group, e.g. "docker:x:999:jenkins"
            def dockerGroupEtcGroup = sh.returnStdOut 'cat /etc/group | grep docker'

            if (dockerGroupEtcGroup.isEmpty()) {
                script.error 'Unable to parse group docker from /etc/group. Docker host will not be accessible for container.'
            }
            return dockerGroupEtcGroup
        }

        private String readDockerGroupId() {
            // Get the GID of the docker group
            sh.returnStdOut "echo ${readDockerGroupFromEtcGroup()} | sed -E 's/.*:x:(.*):.*/\\1/'"
        }

        private void doInstallDockerClient() {
            // Installs statically linked docker binary
            script.sh "cd ${script.pwd()}/.jenkins && wget -qc https://download.docker.com/linux/static/stable/x86_64/docker-$dockerClientVersionToInstall-ce.tgz -O - | tar -xz"
        }
    }
}