package com.cloudogu.ces.cesbuildlib

class K3dRegistry {
    private final String registryName
    private final String localRegistryPort
    private String imageRegistryInternalHandle
    private String imageRegistryExternalHandle
    private Sh sh
    private script
    private Docker docker

    /**
     *
     * @param script
     * @param registryName
     * @param port
     */
    K3dRegistry(def script, Docker docker, String registryName, String port) {
        this.localRegistryPort = port
        this.registryName = registryName
        this.script = script
        this.sh = new Sh(script)
        this.docker = docker
    }

    /**
     * installs a local registry avoiding double resource occupation for TCP ports and registry name
     */
    protected void installLocalRegistry() {
        script.sh "k3d registry create ${this.registryName} --port ${localRegistryPort}"

        this.imageRegistryInternalHandle = "${this.registryName}:${localRegistryPort}"
        this.imageRegistryExternalHandle = "localhost:${localRegistryPort}"
    }

    /**
     *
     * @param imageName
     * @param tag
     * @return
     */
    def buildAndPushToLocalRegistry(String imageName, String tag) {
        def internalHandle="${imageName}:${tag}"
        def externalRegistry="${this.imageRegistryExternalHandle}"

        def dockerImage = docker.build("${internalHandle}")

        docker.withRegistry("http://${externalRegistry}/") {
            dockerImage.push("${tag}")
        }
    }

    def delete() {
        script.sh "k3d registry delete ${this.registryName}"
    }
}
