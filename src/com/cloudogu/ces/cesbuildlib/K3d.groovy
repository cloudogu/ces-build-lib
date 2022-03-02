package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS

class K3d {
    private String workspace
    private String clusterName
    private script
    private String path
    private String k3dBinaryDir

    /**
     * Create an object to set up, modify and tear down a local k3d cluster
     *
     * @param script The Jenkins script you are coming from (aka "this")
     * @param envWorkspace The WORKSPACE environment variable; in Jenkins use "env.WORKSPACE" for example
     * @param envPath The PATH environment variable; in Jenkins use "env.PATH" for example
     */
     K3d(script, String envWorkspace, String envPath) {
        this.workspace = envWorkspace
        this.clusterName = createClusterName()
        this.script = script
        this.path = envPath
        this.k3dBinaryDir = "${workspace}/.k3d/bin"
    }

    /**
     * Creates a randomized cluster name
     *
     * @return new randomized cluster name
     */
    @NonCPS
    static String createClusterName() {
        String[] randomUUIDs = UUID.randomUUID().toString().split("-")
        String uuid_snippet = randomUUIDs[randomUUIDs.length-1]
        // Cluster name must be <= 32 characters
        return "citest-" + uuid_snippet
    }

    /**
     * Starts a k3d cluster in Docker
     * Utilizes code from the cloudogu/gitops-playground
     */
    void startK3d() {
        script.sh "mkdir -p ${k3dBinaryDir}"

        script.git branch: 'main', url: 'https://github.com/cloudogu/gitops-playground'

        script.withEnv(["HOME=${workspace}", "PATH=${k3dBinaryDir}:${path}"]) { // Make k3d write kubeconfig to WORKSPACE
            // Install k3d binary to workspace in order to avoid concurrency issues

            script.sh "if ! command -v k3d >/dev/null 2>&1; then " +
                "curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh |" +
                'TAG=v$(sed -n "s/^K3D_VERSION=//p" scripts/init-cluster.sh) ' +
                "K3D_INSTALL_DIR=${k3dBinaryDir} " +
                'bash -s -- --no-sudo; fi'
            script.sh "yes | ./scripts/init-cluster.sh --cluster-name=${clusterName} --bind-localhost=false"
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
            script.sh "k3d cluster delete ${clusterName}"
        }
    }

    /**
     * Execute a kubectl command on the cluster configured in your workspace
     *
     * @param command The kubectl command you want to execute, without the leading "kubectl"
     */
    void kubectl(command) {
        script.sh "sudo KUBECONFIG=${workspace}/.kube/config kubectl ${command}"
    }

}
