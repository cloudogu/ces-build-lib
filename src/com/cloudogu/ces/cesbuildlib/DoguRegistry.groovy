package com.cloudogu.ces.cesbuildlib

/**
 * This class contain methods and workflows to upload dogus (json) or k8s components (yaml) to a specified dogu registry.
 */
class DoguRegistry {
    public Sh sh
    public HttpClient doguRegistryHttpClient

    private script
    private String backendCredentialsID
    private String doguRegistryURL

    private static String DOGU_POST_ENDPOINT = "api/v2/dogus"
    private static String K8S_POST_ENDPOINT = "api/v1/k8s"

    /**
     * Create an object to upload dogus or k8s components to a specified registry.
     *
     * @param script The Jenkins script you are coming from (aka "this").
     * @param doguRegistryURL Url to the actual dogu registry. Default: 'https://dogu.cloudogu.com'.
     * @param backendCredentialsID Identifier of credentials used to log into the backend. Default: cesmarvin-setup.
     */
    DoguRegistry(script, String doguRegistryURL = "https://dogu.cloudogu.com", String backendCredentialsID = "cesmarvin-setup") {
        this.script = script
        this.backendCredentialsID = backendCredentialsID
        this.doguRegistryURL = doguRegistryURL
        this.doguRegistryHttpClient = new HttpClient(script, backendCredentialsID)
        this.sh = new Sh(script)
    }

    /**
     * Pushes a dogu to the dogu registry.
     *
     * @param pathToDoguJson path to the dogu.json file. The path should be relative to the workspace.
     */
    void pushDogu(String pathToDoguJson = "dogu.json") {
        def doguJson = script.readJSON file: pathToDoguJson
        def doguVersion = doguJson.Version
        def doguNameWithNamespace = doguJson.Name
        script.sh "echo 'Push Dogu:\n-Namespace/Name: ${doguNameWithNamespace}\n-Version: ${doguVersion}'"

        def doguString =  this.sh.returnStdOut("cat ${pathToDoguJson}")
        def trimmedUrl = trimSuffix(doguRegistryURL, '/')
        def result = doguRegistryHttpClient.put("${trimmedUrl}/${DOGU_POST_ENDPOINT}/${doguNameWithNamespace}", "application/json", doguString)
        checkStatus(result, pathToDoguJson)
    }

    /**
     * Pushes a dogu's image to its image registry, then its dogu.json to the dogu registry.
     * For a dogu built elsewhere, e.g. in a CES VM, whose image was loaded into the local docker daemon.
     * Image, version and image registry are read from the dogu.json. Both pushes use the backend credentials.
     *
     * @param doguDir Directory of the dogu.json, relative to the workspace.
     */
    void pushDoguWithImage(String doguDir = ".") {
        String pathToDoguJson = "${doguDir}/dogu.json"
        def doguJson = script.readJSON file: pathToDoguJson
        String image = "${doguJson.Image}:${doguJson.Version}"
        String imageRegistry = doguJson.Image.tokenize('/')[0]
        script.withCredentials([script.usernamePassword(credentialsId: backendCredentialsID, usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
            // Own config dir, so the login doesn't touch the worker's docker config.
            script.withEnv(["DOCKER_CONFIG=${script.pwd(tmp: true)}/docker-push-config", "IMAGE_REGISTRY=${imageRegistry}", "IMAGE=${image}"]) {
                script.sh 'echo "$REGISTRY_PASSWORD" | docker login "$IMAGE_REGISTRY" -u "$REGISTRY_USER" --password-stdin'
                script.sh 'docker push "$IMAGE"'
            }
        }
        pushDogu(pathToDoguJson)
    }

    /**
     * Pushes a yaml tapestry to the dogu registry for k8s components.
     *
     * @param pathToYaml Path to the yaml containing the k8s component.
     * @param k8sName Name of the k8s component.
     * @param k8sNamespace Namespace of the k8s component.
     * @param versionWithoutVPrefix The version of the component without the version prefix.
     */
    void pushK8sYaml(String pathToYaml, String k8sName, String k8sNamespace, String versionWithoutVPrefix) {
        script.sh "echo 'Push Yaml:\n-Name: ${k8sName}\n-Namespace: ${k8sNamespace}\n-Version: ${versionWithoutVPrefix}'"

        def trimmedUrl = trimSuffix(doguRegistryURL, '/')
        def result = doguRegistryHttpClient.putFile("${trimmedUrl}/${K8S_POST_ENDPOINT}/${k8sNamespace}/${k8sName}/${versionWithoutVPrefix}", "application/yaml", pathToYaml)
        checkStatus(result, pathToYaml)
    }

    private static String trimSuffix(String original, String suffix) {
        if(original.endsWith(suffix)) {
            return original.substring(0, original.length() - suffix.length())
        }
        return original
    }

    private void checkStatus(LinkedHashMap<String, Serializable> result, String fileName) {
        def status = result["httpCode"]
        def body = result["body"]

        if ((status as Integer) >= 400) {
            script.sh "echo 'Error pushing ${fileName}'"
            script.sh "echo '${body}'"
            script.sh "exit 1"
        }
    }
}
