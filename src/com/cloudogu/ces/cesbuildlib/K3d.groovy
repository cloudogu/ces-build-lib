package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS

class K3d {
    private String clusterName
    private script
    private String path
    private String k3dDir
    private String k3dBinaryDir
    private Sh sh
    private K3dRegistry registry
    private String registryName

    /**
     * Create an object to set up, modify and tear down a local k3d cluster
     *
     * @param script The Jenkins script you are coming from (aka "this")
     * @param envWorkspace The designated directory for the K3d installation
     * @param envPath The PATH environment variable; in Jenkins use "env.PATH" for example
     */
    K3d(script, String envWorkspace, String envPath) {
        this.clusterName = createClusterName()
        this.registryName = clusterName
        this.script = script
        this.path = envPath
        this.k3dDir = "${envWorkspace}/.k3d"
        this.k3dBinaryDir = "${k3dDir}/bin"
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
        script.sh "rm -rf ${k3dDir}"

        script.withEnv(["HOME=${k3dDir}", "PATH=${k3dBinaryDir}:${path}"]) {
            // Make k3d write kubeconfig to WORKSPACE
            // Install k3d binary to workspace in order to avoid concurrency issues
            String k3dVersion = "4.4.7"
            String tagArgument = "TAG=v${k3dVersion}"
            String tagK3dInstallDir = "K3D_INSTALL_DIR=${k3dBinaryDir}"
            String k3dInstallArguments = "${tagArgument} ${tagK3dInstallDir}"

            script.sh "mkdir -p ${k3dBinaryDir}"

            script.echo "Installing K3d Version: ${k3dVersion}"

            script.sh "curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | ${k3dInstallArguments} bash -s -- --no-sudo"

            installLocalRegistry()
            script.sh "k3d cluster create ${clusterName} "+
                " --k3s-server-arg=--kube-apiserver-arg=service-node-port-range=8010-32767 "+
                " -v /var/run/docker.sock:/var/run/docker.sock@server[0] "+
                " -v /etc/group:/etc/group@server[0] "+
                " -v /tmp:/tmp@server[0] "+
                " --k3s-server-arg=--disable=traefik "+
                " --k3s-server-arg=--disable=servicelb "+
                " --image=rancher/k3s:v1.21.2-k3s1 "+
                " --registry-use ${registry.getImageRegistryInternalWithPort()} " +
                " >/dev/null"

            script.echo "Adding k3d cluster to ~/.kube/config"
            script.sh "k3d kubeconfig merge ${clusterName} --kubeconfig-switch-context > /dev/null"
        }
    }

    /**
     * Installs kubectl via snap
     */
    void installKubectl() {
        script.sh("sudo snap install kubectl --classic")
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
     * installs a local image registry to k3d
     */
    private void installLocalRegistry() {
        def registryPort = findFreeTcpPort()
        def registryName = clusterName
        this.registry = new K3dRegistry(script, registryName, registryPort)
        this.registry.installLocalRegistry()
    }

    /**
     * returns a free, unprivileged TCP port
     *
     * @return new free, unprivileged TCP port
     */
    String findFreeTcpPort() {
        String port = this.sh.returnStdOut('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');')
        return port
    }
}
