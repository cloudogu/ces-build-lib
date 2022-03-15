package com.cloudogu.ces.cesbuildlib

class K3dRegistry {
    private final String registryName
    private final String localRegistryPort
    private String imageRegistryInternalHandle
    private String imageRegistryExternalHandle
    private Sh sh
    private script

    /**
     * creates a new K3dRegistry object
     * @param script the jenkins script
     * @param registryName the name of the local image registry under which images are made available
     * @param port the local registry's TCP port under which images are made available
     */
    K3dRegistry(def script, String registryName, String port) {
        this.localRegistryPort = port
        this.registryName = registryName
        this.script = script
        this.sh = new Sh(script)
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
     * builds an image with the given image name and image tag and pushes it to the local image registry
     * @param imageName the image name
     * @param tag the image tag
     * @return the image repository name of the built image relative to the internal image registry, f. i. localRegistyName:randomPort/my/image:tag
     */
    def buildAndPushToLocalRegistry(def imageName, def tag) {
        def internalHandle="${imageName}:${tag}"
        def externalRegistry="${this.imageRegistryExternalHandle}"

        def dockerImage = script.docker.build("${internalHandle}")

        script.docker.withRegistry("http://${externalRegistry}/") {
            dockerImage.push("${tag}")
        }

        return "${this.imageRegistryInternalHandle}/${internalHandle}"
    }

    /**
     * deletes the local K3d registry
     */
    def delete() {
        try {
            script.sh "k3d registry delete ${this.registryName}"
        } finally {}
    }
}
