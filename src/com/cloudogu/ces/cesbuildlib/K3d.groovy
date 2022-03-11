package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS

class K3d {
    private String gitOpsPlaygroundDir
    private String clusterName
    private script
    private String path
    private String k3dDir
    private String k3dBinaryDir
    private Sh sh
    private Git git

    /**
     * Create an object to set up, modify and tear down a local k3d cluster
     *
     * @param script The Jenkins script you are coming from (aka "this")
     * @param envWorkspace The designated directory for the GitOps playground and K3d installation
     * @param envPath The PATH environment variable; in Jenkins use "env.PATH" for example
     */
    K3d(script, String envWorkspace, String envPath) {
        this.gitOpsPlaygroundDir = envWorkspace
        this.clusterName = createClusterName()
        this.script = script
        this.path = envPath
        this.k3dDir = "${gitOpsPlaygroundDir}/.k3d"
        this.k3dBinaryDir = "${k3dDir}/bin"
        this.sh = new Sh(script)
        this.git = new Git(script)
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
     * Utilizes code from the cloudogu/gitops-playground
     */
    void setupK3d() {
        script.sh "rm -rf ${gitOpsPlaygroundDir}"

        git.executeGit("clone https://github.com/cloudogu/gitops-playground ${gitOpsPlaygroundDir}", true)

        script.withEnv(["HOME=${k3dDir}", "PATH=${k3dBinaryDir}:${path}"]) {
            // Make k3d write kubeconfig to WORKSPACE
            // Install k3d binary to workspace in order to avoid concurrency issues
            String k3dVersion = sh.returnStdOut "sed -n 's/^K3D_VERSION=//p' ${gitOpsPlaygroundDir}/scripts/init-cluster.sh"
            String tagArgument = "TAG=v${k3dVersion}"
            String tagK3dInstallDir = "K3D_INSTALL_DIR=${k3dBinaryDir}"
            String k3dInstallArguments = "${tagArgument} ${tagK3dInstallDir}"

            script.sh "mkdir -p ${k3dBinaryDir}"

            script.echo "Installing K3d Version: ${k3dVersion}"

            script.sh "curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | ${k3dInstallArguments} bash -s -- --no-sudo"
            script.sh "yes | ${gitOpsPlaygroundDir}/scripts/init-cluster.sh --cluster-name=${clusterName} --bind-localhost=false"

            script.echo "Installing kubectl, if not already installed..."
            def kubectlInstallationSuccess = installKubectl()
            if (kubectlInstallationSuccess) {
                script.echo "Kubectl successfully installed"
            } else {
                script.echo "Kubectl installation failed!"
            }
        }
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
        script.sh "sudo KUBECONFIG=${k3dDir}/.kube/config kubectl ${command}"
    }

    boolean installKubectl() {
        def kubectlStatusCode = script.sh script:"snap list kubectl", returnStatus:true
        if (kubectlStatusCode != 0) {
            script.echo "Installing kubectl..."
            def kubectlInstallationStatusCode = script.sh script:"sudo snap install kubectl --classic", returnStatus:true
            if (kubectlInstallationStatusCode == 0) {
                return true
            } else {
                return false
            }
        } else {
            //Kubectl is already installed
            return true
        }
    }
}
