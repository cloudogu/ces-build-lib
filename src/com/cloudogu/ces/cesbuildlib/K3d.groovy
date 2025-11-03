package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput

class K3d {
    /**
     * The image of the k3s version defining the targeted k8s version
     */
    private static String K8S_IMAGE = "rancher/k3s:v1.28.5-k3s1"
    /**
     * The version of k3d to be installed
     */
    private static String K3D_VERSION = "5.6.0"
    private static String K3D_LOG_FILENAME = "k8sLogs"
    private static String K3D_SETUP_JSON_FILE = "k3d_setup.json"
    private static String K3D_VALUES_YAML_FILE = "k3d_values.yaml"
    private static String K3D_BLUEPRINT_FILE = "k3d_blueprint.yaml"
    private static String YQ_VERSION = "4.40.5"

    private String clusterName
    private script
    private String path
    private String k3dDir
    private String k3dBinaryDir
    private String backendCredentialsID
    private String harborCredentialsID
    private String externalIP
    private Sh sh
    private K3dRegistry registry
    private String registryName
    private String workspace
    private Docker docker
    private HttpClient httpClient

    def defaultSetupConfig = [
        adminUsername          : "ces-admin",
        adminPassword          : "ecosystem2016",
        adminGroup             : "CesAdministrators",
        dependencies           : ["official/ldap:2.6.8-4",
                                  "official/cas:7.2.7.4",
                                  "official/postfix:3.10.4.4-1",
                                  "official/usermgt:1.20.0.5"],
        defaultDogu            : "",
        additionalDependencies : [],
        registryConfig         : "",
        registryConfigEncrypted: ""
    ]

    String getRegistryName() {
        return registryName
    }

    /**
     * Create an object to set up, modify and tear down a local k3d cluster
     *
     * @param script The Jenkins script you are coming from (aka "this")
     * @param envWorkspace The designated directory for the K3d installation
     * @param workspace The designated directory for working dir
     * @param envPath The PATH environment variable; in Jenkins use "env.PATH" for example
     * @param backendCredentialsID Identifier of credentials used to log into the backend. Default: cesmarvin-setup
     */
    K3d(script, String workspace, String envWorkspace, String envPath, String backendCredentialsID = "cesmarvin-setup", harborCredentialsID = "harborhelmchartpush") {
        this.clusterName = createClusterName()
        this.registryName = clusterName
        this.script = script
        this.path = envPath
        this.workspace = workspace
        this.k3dDir = "${envWorkspace}/.k3d"
        this.k3dBinaryDir = "${k3dDir}/bin"
        this.backendCredentialsID = backendCredentialsID
        this.harborCredentialsID = harborCredentialsID
        this.sh = new Sh(script)
        this.docker = new Docker(script)
        this.httpClient =  new HttpClient(this, harborCredentialsID)
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
            installHelm()
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
            kubectl("delete secret ces-container-registries || true")

            //create secret for the backend registry
            kubectl("create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"${script.env.TOKEN_ID}\" --from-literal=password=\"${script.env.TOKEN_SECRET}\"")
            kubectl("create secret docker-registry ces-container-registries --docker-server=\"registry.cloudogu.com\" --docker-username=\"${script.env.TOKEN_ID}\" --docker-email=\"a@b.c\" --docker-password=\"${script.env.TOKEN_SECRET}\"")
        }

        script.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: harborCredentialsID, usernameVariable: 'HARBOR_USERNAME', passwordVariable: 'HARBOR_PASSWORD']]) {
            script.sh "echo \"Using credentials: ${harborCredentialsID}\""

            // delete old helm-repo-config if available
            kubectl("delete configmap component-operator-helm-repository || true")
            kubectl("delete secret component-operator-helm-registry || true")

            // create helm-repo-config
            kubectl("create configmap component-operator-helm-repository --from-literal=endpoint=\"registry.cloudogu.com\" --from-literal=schema=\"oci\" --from-literal=plainHttp=\"false\"")

            String auth = script.sh(script: "printf '%s:%s' '${script.env.HARBOR_USERNAME}' '${script.env.HARBOR_PASSWORD}' | base64", returnStdout: true,)
            kubectlHideCommand("create secret generic component-operator-helm-registry --from-literal=config.json='{\"auths\": {\"registry.cloudogu.com\": {\"auth\": \"${auth?.trim()}\"}}}'", false)
        }
    }

    /**
     * Initializes the cluster by creating a respective cluster in k3d.
     */
    private void initializeCluster() {
        script.sh "k3d cluster create ${clusterName} " +
            // Allow services to bind to ports < 30000
            " --k3s-arg=--kube-apiserver-arg=service-node-port-range=8010-32767@all:* " +
            // Used by Jenkins Agents pods
            " -v /var/run/docker.sock:/var/run/docker.sock@server:0 " +
            // Allows for finding out the GID of the docker group in order to allow the Jenkins agents pod to access docker socket
            " -v /etc/group:/etc/group@server:0 " +
            // Persists the cache of Jenkins agents pods for faster builds
            " -v /tmp:/tmp@server:0 " +
            // Disable traefik (no ingresses used so far)
            //" --k3s-arg=--disable=traefik@all:* " +a
            // Disable servicelb (avoids "Pending" svclb pods and we use nodePorts right now anyway)
            //" --k3s-arg=--disable=servicelb@all:* " +
            // Pin k8s version to 1.21.2
            " --image=${K8S_IMAGE} " +
            // Use our k3d registry
            " --registry-use ${registry.getImageRegistryInternalWithPort()} " +
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
        kubectl(command, false)
    }

    String kubectl(command, returnStdout) {
        return script.sh(script: "sudo KUBECONFIG=${k3dDir}/.kube/config kubectl ${command}", returnStdout: returnStdout)
    }

    /**
     * Runs any helm command.
     * @param command must contain all necessary arguments and flags.
     */
    void helm(command) {
        helm(command, false)
    }

    /**
     * Runs any helm command and returns the generated output if configured.
     * @param command must contain all necessary arguments and flags.
     * @param returnStdout if set to true this method returns the standard output stream generated by helm.
     */
    String helm(command, returnStdout) {
        return script.sh(script: "sudo KUBECONFIG=${k3dDir}/.kube/config helm ${command}", returnStdout: returnStdout)
    }

    String kubectlHideCommand(command, returnStdout) {
        return script.sh(script: "set +x; sudo KUBECONFIG=${k3dDir}/.kube/config kubectl ${command}", returnStdout: returnStdout)
    }


    void assignExternalIP() {
        this.externalIP = this.sh.returnStdOut("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip")
    }

    void createEmptySetupValuesYamlIfItDoesNotExists() {
        if (!script.fileExists(K3D_VALUES_YAML_FILE)) {
            script.echo "create values.yaml for setup deployment"
            script.writeFile file: K3D_VALUES_YAML_FILE, text: ""
        }
    }

    void yqEvalYamlFile(String file, String eval) {
        createEmptySetupValuesYamlIfItDoesNotExists()
        doInYQContainer {
            script.sh("yq -i \"${eval}\" ${file}")
        }
    }

    void appendToYamlFile(String file, String key, String value) {
        yqEvalYamlFile(file, "${key} = \\\"${value}\\\"")
    }

    void appendFileToYamlFile(String file, String key, String fileName) {
        createEmptySetupValuesYamlIfItDoesNotExists()
        doInYQContainer {
            script.sh("yq -i '${key} = load_str(\"${fileName}\")' ${file}")
        }
    }

    void doInYQContainer(Closure closure) {
        docker.image("mikefarah/yq:${YQ_VERSION}")
            .mountJenkinsUser().inside("--volume ${this.workspace}:/workdir -w /workdir") {
            closure.call()
        }
    }

    void configureEcosystemCoreValues(config = [:]) {

        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".defaultConfig.env.waitTimeoutMinutes = 5")
        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-dogu-operator-crd.version", "2.10.0")
        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-dogu-operator.version", "3.15.0")

        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-blueprint-operator-crd.version", "2.0.1")
        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-blueprint-operator.version", "3.0.0-CR1")

        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-blueprint-operator.valuesObject.healthConfig.components.required = [{\\\"name\\\": \\\"k8s-dogu-operator\\\"}, {\\\"name\\\": \\\"k8s-service-discovery\\\"}]")


        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-service-discovery.valuesObject.loadBalancerService.internalTrafficPolicy", "Cluster")
        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-service-discovery.valuesObject.loadBalancerService.externalTrafficPolicy", "Cluster")

        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".backup.enabled = false")
        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".monitoring.enabled = false")


        script.echo "configuring ecosystem core..."
        // Merge default config with the one passed as parameter
        config = defaultSetupConfig << config
        writeBlueprintYaml(config)
    }

    @Deprecated
    void configureSetupJson(config = [:]) {
        configureEcosystemCoreValues(config)
    }

    void configureSetupImage(String image) {
        String hostKey = ".setup.image.registry"
        String repositoryKey = ".setup.image.repository"
        String tagKey = ".setup.image.tag"
        def repositorySeparatorIndex = image.indexOf("/")
        def tagSeparatorIndex = image.lastIndexOf(":")

        appendToYamlFile(K3D_VALUES_YAML_FILE, hostKey, image.substring(0, repositorySeparatorIndex))
        appendToYamlFile(K3D_VALUES_YAML_FILE, repositoryKey, image.substring(repositorySeparatorIndex + 1, tagSeparatorIndex))
        appendToYamlFile(K3D_VALUES_YAML_FILE, tagKey, image.substring(tagSeparatorIndex + 1, image.length()))
    }

    void configureComponentOperatorVersion(String operatorVersion, String crdVersion = operatorVersion, String namespace = "k8s") {
        String componentOpKey = ".component_operator_chart"
        String componentCRDKey = ".component_operator_crd_chart"


        def builder = new StringBuilder(namespace)
        String operatorValue = builder.append("/k8s-component-operator:").append(operatorVersion).toString()
        appendToYamlFile(K3D_VALUES_YAML_FILE, componentOpKey, operatorValue)
        builder.delete(0, builder.length());
        String crdValue = builder.append(namespace).append("/k8s-component-operator-crd:").append(crdVersion).toString()
        appendToYamlFile(K3D_VALUES_YAML_FILE, componentCRDKey, crdValue)
    }

    void configureComponents(components = [:]) {
        def evals = []
        components.each { componentName, componentConfig ->
            def disableComponent=componentConfig == null
            if (disableComponent) {
                evals << ".components.${componentName} = null"
            } else {
                componentConfig.each { configKey, configValue ->
                    evals << ".components.${componentName}.${configKey} = \\\"${configValue}\\\""
                }
            }
        }

        if (evals.size() > 0) {
            yqEvalYamlFile(K3D_VALUES_YAML_FILE, evals.join(" | "))
        }
    }

    void configureLogLevel(String loglevel) {
        appendToYamlFile(K3D_VALUES_YAML_FILE, ".logLevel", loglevel)
    }

    void installAndTriggerSetup(String tag, Integer timeout = 300, Integer interval = 5) {
        script.echo "Installing setup..."
        String registryUrl = "registry.cloudogu.com"
        String registryNamespace = "k8s"
        script.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'harborhelmchartpush', usernameVariable: 'HARBOR_USERNAME', passwordVariable: 'HARBOR_PASSWORD']]) {
            helm("registry login ${registryUrl} --username '${script.env.HARBOR_USERNAME}' --password '${script.env.HARBOR_PASSWORD}'")
        }

        // install crd first
        helm("install k8s-component-operator-crd oci://${registryUrl}/${registryNamespace}/k8s-component-operator-crd  --version 1.10.0 --namespace default")

        kubectl("--namespace default create configmap global-config --from-literal=config.yaml='fqdn: ${externalIP}'")

        helm("install -f ${K3D_VALUES_YAML_FILE} ecosystem-core oci://${registryUrl}/${registryNamespace}/ecosystem-core --version 0.4.0 --namespace default --timeout 15m")

        script.echo "Wait for blueprint-operator to be ready..."
        waitForDeploymentRollout("k8s-blueprint-operator-controller-manager", timeout, interval)

        kubectl("apply -f ${K3D_BLUEPRINT_FILE} --namespace default")

        helm("registry logout ${registryUrl}")

        script.echo "Wait for setup-finisher to be executed..."
        waitForSetupToFinish(timeout, interval)

        script.echo "Wait for dogus to be ready..."
        waitForDogusToBeRolledOut(timeout, interval)
    }

    void waitForDogusToBeRolledOut(Integer timeout, Integer interval) {
        String dogus = kubectl("get dogus --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", true)
        String[] doguList = dogus.trim().split("\n")
        for (String dogu : doguList) {
            script.echo "Wait for $dogu to be rolled out..."
            waitForDeploymentRollout(dogu, timeout, interval)
        }
    }

    void waitForSetupToFinish(Integer timeout, Integer interval) {
        for (int i = 0; i < timeout / interval; i++) {
            script.sh("sleep ${interval}s")
            String blueprintReady = kubectl("get blueprint -n=default blueprint-ces-module -o jsonpath='{.status.conditions[?(@.type==\"EcosystemHealthy\")].status}{\" \"}{.status.conditions[?(@.type==\"Completed\")].status}'", true)
            String dogus = kubectl("get dogus --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", true)
            script.echo blueprintReady
            script.echo dogus
            if (blueprintReady == "True True") {
                return
            }
        }

        this.script.error "failed to wait for ecosystem-core setup to finish: timeout"
    }

/**
 * Installs the setup to the cluster. Creates an example setup.json with usermgt as dogu and executes the setup.
 * After that the method will wait until the dogu-operator is ready.
 * @param tag Tag of the setup e. g. "v0.6.0"
 * @param timout Timeout in seconds for the setup process e. g. 300
 * @param interval Interval in seconds for querying the actual state of the setup e. g. 2
 */
    void setup(String tag, config = [:], Integer timout = 300, Integer interval = 5) {
        assignExternalIP()
        configureEcosystemCoreValues(config)
        installAndTriggerSetup(tag, timout, interval)
    }


/**
 * Installs a given dogu. Before applying the dogu.yaml to the cluster the method creates a custom dogu
 * descriptor with an .local ip. This is required for the crane library in the dogu operator. The .local forces it
 * to use http. Afterwards the .local will be patched out from the dogu resources so that kubelet has no problem to
 * pull the image-
 *
 * @param dogu Name of the dogu e. g. "nginx-ingress"
 * @param image Name of the image e. g. "k3d-citest-d9753c5632bc:1234/k8s/nginx-ingress"
 * @param doguYaml Name of the custom resources
 */
    void installDogu(String dogu, String image, String doguYaml) {
        String[] IpPort = getRegistryIpAndPort()
        String imageUrl = image.split(":")[0]
        patchCoreDNS(IpPort[0], imageUrl)

        applyDevDoguDescriptor(dogu, imageUrl, IpPort[1])
        kubectl("apply -f ${doguYaml}")

        // Remove .local from Images.
        patchDoguExecPod(dogu, image)
        patchDoguDeployment(dogu, image)
    }

/**
 * Applies the specified dogu resource into the k8s cluster. This should be used for dogus which are not build or
 * locally installed in the build process. An example for the usage would be to install a dogu dependency before
 * starting integration tests.
 *
 * @param doguName Name of the dogu, e.g., "nginx-ingress"
 * @param doguNamespace Namespace of the dogu, e.g., "official"
 * @param doguVersion Version of the dogu, e.g., "13.9.9-1"
 */
    void applyDoguResource(String doguName, String doguNamespace, String doguVersion) {
        def filename = "target/make/k8s/${doguName}.yaml"
        def doguContentYaml = """
apiVersion: k8s.cloudogu.com/v2
kind: Dogu
metadata:
  name: ${doguName}
  labels:
    dogu: ${doguName}
spec:
  name: ${doguNamespace}/${doguName}
  version: ${doguVersion}
"""

        script.writeFile(file: filename.toString(), text: doguContentYaml.toString())
        kubectl("apply -f ${filename}")
    }

    private void applyDevDoguDescriptor(String dogu, String imageUrl, String port) {
        String imageDev
        String doguJsonDevFile = "${this.workspace}/target/dogu.json"
        docker.image("mikefarah/yq:${YQ_VERSION}")
            .mountJenkinsUser()
            .inside("--volume ${this.workspace}:/workdir -w /workdir") {
                imageDev = this.sh.returnStdOut("yq -oy -e '.Image' dogu.json | sed 's|registry\\.cloudogu\\.com\\(.\\+\\)|${imageUrl}.local:${port}\\1|g'")
                script.sh "yq -oj '.Image=\"${imageDev}\"' dogu.json > ${doguJsonDevFile}"
            }
        kubectl("create configmap ${dogu}-descriptor --from-file=${doguJsonDevFile}")
    }

    private void patchDoguExecPod(String dogu, String image) {
        String execPodName = getExecPodName(dogu, 30, 2)
        if (execPodName == "") {
            return
        }
        kubectl("patch pod '${execPodName}' -p '{\"spec\":{\"containers\":[{\"name\":\"${execPodName}\",\"image\":\"${image}\"}]}}'")
    }

    private void patchDoguDeployment(String dogu, String image) {
        waitForDeployment(dogu, 300, 5)
        kubectl("patch deployment '${dogu}' -p '{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"${dogu}\",\"image\":\"${image}\"}]}}}}'")
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
/**
 * Installs helm
 */
    void installHelm() {
        def helmStatusCode = script.sh script: "snap list helm", returnStatus: true
        if (helmStatusCode == 0 || helmStatusCode.equals("0")) {
            script.echo "helm already installed"
            return
        }

        script.echo "Installing helm..."
        script.sh script: "sudo snap install helm --classic"
    }

    private String getExecPodName(String dogu, Integer timeout, Integer interval) {
        for (int i = 0; i < timeout / interval; i++) {
            script.sh("sleep ${interval}s")
            try {
                String podList = kubectl("get pod --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", true)
                String execPodName = this.sh.returnStdOut("echo '${podList}' | grep '${dogu}-execpod'")
                if (execPodName.contains("${dogu}-execpod")) {
                    return execPodName
                }
            } catch (ignored) {
                // Ignore Error.
            }
        }

        // Don't throw an error here because in the future the won't be exec pods for every dogu
        return ""
    }

    void waitForDeployment(String deployment, Integer timeout, Integer interval) {
        for (int i = 0; i < timeout / interval; i++) {
            script.sh("sleep ${interval}s")
            try {
                String deploymentList = kubectl("get deployment --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", true)
                String statusMsg = this.sh.returnStdOut("echo '${deploymentList}' | grep '${deployment}'")
                if (statusMsg == deployment) {
                    return
                }
            } catch (ignored) {
                // Ignore Error.
            }
        }

        this.script.error "failed to wait for deployment/${deployment}: timeout"
    }

    void waitForDeploymentRollout(String deployment, Integer timeout, Integer interval) {
        for (int i = 0; i < timeout / interval; i++) {
            script.sh("sleep ${interval}s")
            try {
                String statusMsg = kubectl("rollout status deployment/${deployment}", true)
                if (statusMsg.contains("successfully rolled out")) {
                    return
                }
            } catch (ignored) {
                // If the deployment does not even exists an error will be thrown. Ignore this case.
            }
        }

        this.script.error "failed to wait for deployment/${deployment} rollout: timeout"
    }

    private void patchCoreDNS(String ip, String imageUrl) {
        String fileName = "coreDNSPatch.yaml"
        script.writeFile file: fileName, text: """
data:
    Corefile: |
        ${imageUrl}.local:53 {
            hosts {
                ${ip} ${imageUrl}.local
            }
        }
        .:53 {
            errors
            health
            ready
            kubernetes cluster.local in-addr.arpa ip6.arpa {
                pods insecure
                fallthrough in-addr.arpa ip6.arpa
            }
            hosts /etc/coredns/NodeHosts {
                reload 1s
                fallthrough
            }
            prometheus :9153
            forward . /etc/resolv.conf
            cache 30
            loop
            reload
            loadbalance
        }
"""

        kubectl("-n kube-system patch cm coredns --patch-file ${fileName}")
        kubectl("rollout restart -n kube-system deployment/coredns")
    }

    private String[] getRegistryIpAndPort() {
        String registryIp
        String registryPortProtocol
        String prefixedRegistryName = "k3d-${this.registryName}"
        String dockerInspect = script.sh(script: "docker inspect ${prefixedRegistryName}", returnStdout: true)
        docker.image("mikefarah/yq:${YQ_VERSION}")
            .mountJenkinsUser().inside("--volume ${this.workspace}:/workdir -w /workdir") {
            registryIp = script.sh(script: "echo '${dockerInspect}' | yq '.[].NetworkSettings.Networks.${prefixedRegistryName}.IPAddress'", returnStdout: true).trim()
            registryPortProtocol = script.sh(script: "echo '${dockerInspect}' | yq '.[].Config.Labels.\"k3s.registry.port.internal\"'", returnStdout: true).trim()
        }
        String registryPort = registryPortProtocol.split("/")[0]

        return [registryIp, registryPort]
    }

    String formatDependencies(List<String> deps) {
        String formatted = ""
        for (int i = 0; i < deps.size(); i++) {
            String[] parts = deps[i].split(":")
            script.echo "DEP: '${deps[i]}'"
            String version = "";
            if (parts.length != 2 || parts[1] == "latest") {
                def response = httpClient.get("https://dogu.cloudogu.com/api/v2/dogus/${parts[0]}/_versions")
                def versions = script.readJSON text: response["body"], returnPojo: true
                version = version[0]
            } else {
                version = parts[1]
            }
            formatted += "      - name: ${parts[0]}\n" +
                "        version: ${version}"
            if ((i + 1) < deps.size()) {
                formatted += '\n'
            }
        }

        return formatted
    }

    private void writeBlueprintYaml(config) {
        List<String> deps = config.dependencies + config.additionalDependencies
        String formattedDeps = formatDependencies(deps)
        script.writeFile file: K3D_BLUEPRINT_FILE, text: """
apiVersion: k8s.cloudogu.com/v2
kind: Blueprint
metadata:
  labels:
    app: ces
    app.kubernetes.io/name: k8s-blueprint-lib
  name: blueprint-ces-module
  namespace: default
spec:
  displayName: "Blueprint Terraform CES-Module"
  blueprint:
    dogus:
${formattedDeps}
    config:
      dogus:
        ldap:
          - key: admin_username
            value: "${config.adminUsername}"
          - key: admin_mail
            value: "ces-admin@cloudogu.com"
          - key: admin_member
            value: "true"
          - key: admin_password
            value: "${config.adminPassword}"
      global:
        - key: fqdn
          value: "${externalIP}"
        - key: domain
          value: "ces.local"
        - key: certificate/type
          value: "selfsigned"
        - key: k8s/use_internal_ip
          value: "false"
        - key: internalIp
          value: ""
        - key: admin_group
          value: "${config.adminGroup}"
"""
    }

    private void writeSetupJson(config) {
        List<String> deps = config.dependencies + config.additionalDependencies
        String formattedDeps = formatDependencies(deps)

        script.writeFile file: K3D_SETUP_JSON_FILE, text: """
{
  "naming":{
    "fqdn":"${externalIP}",
    "hostname":"ces",
    "domain":"ces.local",
    "certificateType":"selfsigned",
    "relayHost":"mail.ces.local",
    "completed":true
  },
  "dogus":{
    "defaultDogu":"${config.defaultDogu}",
    "install":[
       ${formattedDeps}
    ],
    "completed":true
  },
  "admin":{
    "username":"${config.adminUsername}",
    "mail":"ces-admin@cloudogu.com",
    "password":"${config.adminPassword}",
    "adminGroup":"${config.adminGroup}",
    "adminMember":true,
    "completed":true
  },
  "userBackend":{
    "port":"389",
    "useUserConnectionToFetchAttributes":true,
    "dsType":"embedded",
    "attributeID":"uid",
    "attributeFullname":"cn",
    "attributeMail":"mail",
    "attributeGroup":"memberOf",
    "searchFilter":"(objectClass=person)",
    "host":"ldap",
    "completed":true
  },
  "registryConfig": {${config.registryConfig}},
  "registryConfigEncrypted": {${config.registryConfigEncrypted}}
}"""
    }


/**
 * Collects all necessary resources and log information used to identify problems with our kubernetes cluster.
 *
 * The collected information are archived as zip files at the build.
 */
    void collectAndArchiveLogs() {
        script.dir(K3D_LOG_FILENAME) {
            script.deleteDir()
        }
        script.sh("rm -rf ${K3D_LOG_FILENAME}.zip".toString())
        script.sh("rm -rf ${K3D_SETUP_JSON_FILE}".toString())
        //script.sh("rm -rf ${K3D_BLUEPRINT_FILE}".toString())
        //script.sh("rm -rf ${K3D_VALUES_YAML_FILE}".toString())

        collectResourcesSummaries()
        collectDoguDescriptions()
        collectPodLogs()

        String fileNameString = "${K3D_LOG_FILENAME}.zip".toString()
        script.zip(zipFile: fileNameString, archive: "false", dir: "${K3D_LOG_FILENAME}".toString())
        script.archiveArtifacts(artifacts: fileNameString, allowEmptyArchive: "true")
    }

/**
 * Collects all information about resources and their quantity and saves them as .yaml files.
 */
    void collectResourcesSummaries() {
        def relevantResources = [
            "persistentvolumeclaim",
            "statefulset",
            "replicaset",
            "deployment",
            "service",
            "secret",
            "pod",
            "configmap",
            "persistentvolume",
            "ingress",
            "ingressclass"
        ]

        for (def resource : relevantResources) {
            def resourceYaml = kubectl("get ${resource} --show-kind --ignore-not-found -l app=ces -o yaml || true", true)
            script.dir("${K3D_LOG_FILENAME}") {
                script.writeFile(file: "${resource}.yaml".toString(), text: resourceYaml)
            }
            def resourceDescription = kubectl("describe ${resource} -l app=ces || true", true)
            script.dir("${K3D_LOG_FILENAME}") {
                script.writeFile(file: "${resource}_description.yaml".toString(), text: resourceDescription)
            }
        }
    }

/**
 * Collects all descriptions of dogus resources and saves them as .yaml files into the k8s logs directory.
 */
    void collectDoguDescriptions() {
        def allDoguNames = kubectl("get dogu --ignore-not-found -o name || true", true)
        try {
            def doguNames = allDoguNames.split("\n")
            for (def doguName : doguNames) {
                def doguFileName = doguName.split("/")[1]
                def doguDescribe = kubectl("describe ${doguName} || true", true)
                script.dir("${K3D_LOG_FILENAME}") {
                    script.dir('dogus') {
                        script.writeFile(file: "${doguFileName}.txt".toString(), text: doguDescribe)
                    }
                }
            }
        } catch (Exception ignored) {
            script.echo "Failed to collect dogu descriptions because of: \n${ignored.toString()}\nSkipping collection step."
        }
    }

/**
 * Collects all pod logs and saves them into the k8s logs directory.
 */
    void collectPodLogs() {
        def allPodNames = kubectl("get pods -o name || true", true)
        try {
            def podNames = allPodNames.split("\n")
            for (def podName : podNames) {
                def podFileName = podName.split("/")[1]
                def podLogs = kubectl("logs ${podName} || true", true)
                script.dir("${K3D_LOG_FILENAME}") {
                    script.dir('pods') {
                        script.writeFile(file: "${podFileName}.log".toString(), text: podLogs)
                    }
                }
            }
        } catch (Exception ignored) {
            script.echo "Failed to collect pod logs because of: \n${ignored.toString()}\nSkipping collection step."
        }
    }

}
