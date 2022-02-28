package com.cloudogu.ces.cesbuildlib

class K3d {
    private String workspace
    private String clusterName
    private script
    Sh sh

    K3d(String workspace, String clusterName) {
        this.workspace = workspace
        this.clusterName = clusterName
        this.script = script
        this.sh = new Sh(script)
    }

    /**
     * Creates a randomized cluster name
     *
     * @return new randomized cluster name
     */
    String createClusterName() {
        String[] randomUUIDs = UUID.randomUUID().toString().split("-")
        String uuid = randomUUIDs[randomUUIDs.length-1]
        return "citest-" + uuid
    }

    /**
     * Starts a k3d cluster in Docker
     * Utilizes code from the cloudogu/gitops-playground
     *
     * @param clusterName The name your cluster should get
     */
    void startK3d(String clusterName) {
        sh.returnStdOut "mkdir -p ${this.workspace}/.k3d/bin"

        git branch: 'main', url: 'https://github.com/cloudogu/gitops-playground'

        withEnv(["HOME=${this.workspace}", "PATH=${this.workspace}/.k3d/bin:"+'${PATH}']) { // Make k3d write kubeconfig to WORKSPACE
            // Install k3d binary to workspace in order to avoid concurrency issues
            sh.returnStdOut "if ! command -v k3d >/dev/null 2>&1; then " +
                "curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh |" +
                'TAG=v$(sed -n "s/^K3D_VERSION=//p" scripts/init-cluster.sh) ' +
                "K3D_INSTALL_DIR=${this.workspace}/.k3d/bin " +
                'bash -s -- --no-sudo; fi'
            sh.returnStdOut "yes | ./scripts/init-cluster.sh --cluster-name=${clusterName} --bind-localhost=false"
        }
    }

    /**
     * Delete a k3d cluster in Docker
     *
     * @param clusterName The name of the cluster you want to delete
     */
    void deleteK3d(String clusterName) {
        withEnv(["PATH=${this.workspace}/.k3d/bin:"+'${PATH}']) {
            sh.returnStdOut "k3d cluster delete ${clusterName}"
        }
    }

    /**
     * Execute a kubectl command on the cluster configured in your workspace
     *
     * @param command The kubectl command you want to execute, without the leading "kubectl"
     */
    void kubectl(command) {
        sh.returnStdOut "sudo KUBECONFIG=${this.workspace}/.kube/config kubectl ${command}"
    }

}
