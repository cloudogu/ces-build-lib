package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS

class K3d {
    /**
     * The image of the k3s version defining the targeted k8s version
     */
    private static String K8S_IMAGE = "rancher/k3s:v1.21.2-k3s1"
    /**
     * The version of k3d to be installed
     */
    private static String K3D_VERSION = "4.4.7"

    private String clusterName
    private script
    private String path
    private String k3dDir
    private String k3dBinaryDir
    private String backendCredentialsID
    private Sh sh
    private K3dRegistry registry
    public String registryName

    /**
     * Create an object to set up, modify and tear down a local k3d cluster
     *
     * @param script The Jenkins script you are coming from (aka "this")
     * @param envWorkspace The designated directory for the K3d installation
     * @param envPath The PATH environment variable; in Jenkins use "env.PATH" for example
     * @param backendCredentialsID Identifier of credentials used to log into the backend. Default: cesmarvin-setup
     */
    K3d(script, String envWorkspace, String envPath, String backendCredentialsID="cesmarvin-setup") {
        this.clusterName = createClusterName()
        this.registryName = clusterName
        this.script = script
        this.path = envPath
        this.k3dDir = "${envWorkspace}/.k3d"
        this.k3dBinaryDir = "${k3dDir}/bin"
        this.backendCredentialsID = backendCredentialsID
        this.sh = new Sh(script)
    }

    /**
     * Creates a randomized cluster name
     *
     * @return new randomized cluster name
     */
    @NonCPS
    static String createClusterName() {
        String[] randomUUIDs = UUID.randomUUID().toString().split("-")
        String uuid_snippet = randomUUIDs[randomUUIDs.length - 1]
        // Cluster name must be <= 32 characters
        return "citest-" + uuid_snippet
    }

    /**
     * Starts a k3d cluster in Docker
     */
    void startK3d() {
        // Make k3d write kubeconfig to WORKSPACE
        // Install k3d binary to workspace in order to avoid concurrency issues
        script.withEnv(["HOME=${k3dDir}", "PATH=${k3dBinaryDir}:${path}"]) {
            installK3d()
            installLocalRegistry()
            initializeCluster()
            installKubectl()
            loginBackend()
        }
    }

    /**
     * Creates the secret necessary for applications that need to log into the cloudogu backend, e.g., dogu-operator.
     */
    private void loginBackend() {
        script.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: backendCredentialsID, usernameVariable: 'TOKEN_ID', passwordVariable: 'TOKEN_SECRET']]) {
            script.sh "echo \"Using credentials: ${backendCredentialsID}\""

            // delete old secrets if available
            kubectl("delete secret k8s-dogu-operator-dogu-registry || true")
            kubectl("delete secret k8s-dogu-operator-docker-registry || true")

            //create secret for the backend registry
            kubectl("create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"${script.env.TOKEN_ID}\" --from-literal=password=\"${script.env.TOKEN_SECRET}\"")
            kubectl("create secret docker-registry k8s-dogu-operator-docker-registry --docker-server=\"registry.cloudogu.com\" --docker-username=\"${script.env.TOKEN_ID}\" --docker-email=\"a@b.c\" --docker-password=\"${script.env.TOKEN_SECRET}\"")
        }
    }

    /**
     * Initializes the cluster by creating a respective cluster in k3d.
     */
    private void initializeCluster() {
        def dockerInspect = sh(script: "docker inspect k3d-${this.registryName}", returnStdout: true)
        def registryIp

        Docker docker = new Docker(this)

        docker.image('mikefarah/yq:4.22.1')
            .mountJenkinsUser()
            .inside("--volume ${WORKSPACE}:/workdir -w /workdir") {
                registryIp = sh(script: "echo '${dockerInspect}' | yq '.[].NetworkSettings.Networks.k3d-${this.registryName}.IPAddress'", returnStdout: true).trim()
            }

        sh "echo testttttttttttttttttt lib"
        sh "echo ${registryIp}"

        script.writeFile file:'registry_config.yaml', text: """
mirrors:
  ${registryIp}:5000:
    endpoint:
      - http://${registryIp}:5000
configs:
  ${registryIp}:
    tls:
      insecure_skip_verify: true
"""
        script.sh "k3d cluster create ${clusterName} " +
            // Allow services to bind to ports < 30000
            " --k3s-server-arg=--kube-apiserver-arg=service-node-port-range=8010-32767 " +
            // Used by Jenkins Agents pods
            " -v /var/run/docker.sock:/var/run/docker.sock@server[0] " +
            // Allows for finding out the GID of the docker group in order to allow the Jenkins agents pod to access docker socket
            " -v /etc/group:/etc/group@server[0] " +
            // Persists the cache of Jenkins agents pods for faster builds
            " -v /tmp:/tmp@server[0] " +
            // Disable traefik (no ingresses used so far)
            " --k3s-server-arg=--disable=traefik " +
            // Disable servicelb (avoids "Pending" svclb pods and we use nodePorts right now anyway)
            " --k3s-server-arg=--disable=servicelb " +
            // Pin k8s version to 1.21.2
            " --image=${K8S_IMAGE} " +
            // Use our k3d registry
            " --registry-use ${registry.getImageRegistryInternalWithPort()} " +
            // TODO Test
            "--registry-config registry_config.yaml" +
            " >/dev/null"


        script.echo "Adding k3d cluster to ~/.kube/config"
        script.sh "k3d kubeconfig merge ${clusterName} --kubeconfig-switch-context > /dev/null"
    }

    /**
     * Delete a k3d cluster in Docker
     */
    void deleteK3d() {
        script.withEnv(["PATH=${k3dBinaryDir}:${path}"]) {
            script.echo "Deleting cluster registry..."
            this.registry?.delete()

            script.echo "Deleting cluster..."
            script.sh "k3d cluster delete ${clusterName}"
        }
    }

    /**
     * Builds a local image and pushes it to the local registry
     * @param imageName the image name without the local registry/port parts, f. e. "cloudogu/myimage"
     * @param tag the image tag, f. e. "1.2.3"
     * @return the image repository name of the built image relative to the internal image registry, f. i. localRegistyName:randomPort/my/image:tag
     */
    def buildAndPushToLocalRegistry(def imageName, def tag) {
        return this.registry.buildAndPushToLocalRegistry(imageName, tag)
    }

    /**
     * Execute a kubectl command on the cluster configured in your workspace
     *
     * @param command The kubectl command you want to execute, without the leading "kubectl"
     */
    void kubectl(command) {
        script.sh "sudo KUBECONFIG=${k3dDir}/.kube/config kubectl ${command}"
    }

    /**
     * returns a free, unprivileged TCP port
     *
     * @return new free, unprivileged TCP port
     */
    private String findFreeTcpPort() {
        String port = this.sh.returnStdOut('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');')
        return port
    }

    /**
     * installs a local image registry to k3d
     */
    private void installLocalRegistry() {
        def registryPort = findFreeTcpPort()
        def registryName = clusterName
        this.registry = new K3dRegistry(script, registryName, registryPort)
        this.registry.installLocalRegistry()
    }

    /**
     * Installs k3d
     */
    private void installK3d() {
        script.sh "rm -rf ${k3dDir}"
        script.sh "mkdir -p ${k3dBinaryDir}"

        String tagArgument = "TAG=v${K3D_VERSION}"
        String tagK3dInstallDir = "K3D_INSTALL_DIR=${k3dBinaryDir}"
        String k3dInstallArguments = "${tagArgument} ${tagK3dInstallDir}"

        script.echo "Installing K3d Version: ${K3D_VERSION}"
        script.sh "curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | ${k3dInstallArguments} bash -s -- --no-sudo"
    }

    /**
     * Installs kubectl
     */
    private void installKubectl() {
        def kubectlStatusCode = script.sh script: "snap list kubectl", returnStatus: true
        if (kubectlStatusCode == 0) {
            script.echo "Kubectl already installed"
            return
        }

        script.echo "Installing kubectl..."
        script.sh script: "sudo snap install kubectl --classic"
    }
}
