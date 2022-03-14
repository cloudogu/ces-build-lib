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
    private K3dRegistry registry
    private String registryName
    private Docker docker

    /**
     * Create an object to set up, modify and tear down a local k3d cluster
     *
     * @param script The Jenkins script you are coming from (aka "this")
     * @param envWorkspace The designated directory for the GitOps playground and K3d installation
     * @param envPath The PATH environment variable; in Jenkins use "env.PATH" for example
     * @param gitCredentials credentials used for checking out the GitOps playground
     */
    K3d(script, Docker docker, String envWorkspace, String envPath, String gitCredentials) {
        this.gitOpsPlaygroundDir = envWorkspace
        this.clusterName = createClusterName()
        this.registryName = clusterName
        this.script = script
        this.path = envPath
        this.k3dDir = "${gitOpsPlaygroundDir}/.k3d"
        this.k3dBinaryDir = "${k3dDir}/bin"
        this.sh = new Sh(script)
        this.git = new Git(script, gitCredentials)
        this.docker = docker
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
    void startK3d() {
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

            installLocalRegistry()
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
            try {
                script.echo "Deleting cluster registry..."
                this.registry.delete()
            } finally {
                script.echo "Deleting cluster..."
                script.sh "k3d cluster delete ${clusterName}"
            }
        }
    }

    void buildAndPushToLocalRegistry(String imageName, String tag) {
        this.registry.buildAndPushToLocalRegistry(imageName, tag)
    }
    /**
     * Execute a kubectl command on the cluster configured in your workspace
     *
     * @param command The kubectl command you want to execute, without the leading "kubectl"
     */
    void kubectl(command) {
        script.sh "sudo KUBECONFIG=${k3dDir}/.kube/config kubectl ${command}"
    }

    void installLocalRegistry() {
        def registryPort = findFreeTcpPort(sh)
        def registryName = clusterName
        this.registry = new K3dRegistry(script, docker, registryName, registryPort)
        this.registry.installLocalRegistry()
    }

    /**
     * returns a free, unprivileged TCP port
     *
     * @return new free, unprivileged TCP port
     */
    String findFreeTcpPort(Sh sh) {
        // based on https://unix.stackexchange.com/a/358101/440116 which uses only basic unix tools
        return sh.returnStdOut("netstat -aln | awk '\n" +
            "  \$6 == \"LISTEN\" {\n" +
            "    if (\$4 ~ \"[.:][0-9]+\$\") {\n" +
            "      split(\$4, a, /[:.]/);\n" +
            "      port = a[length(a)];\n" +
            "      p[port] = 1\n" +
            "    }\n" +
            "  }\n" +
            "  END {\n" +
            "    for (i = 3000; i < 65000 && p[i]; i++){};\n" +
            "    if (i == 65000) {exit 1};\n" +
            "    print i\n" +
            "  }\n" +
            "'")
    }
}
