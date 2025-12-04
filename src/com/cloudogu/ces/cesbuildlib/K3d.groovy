package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper

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
    private static String K3D_VALUES_YAML_FILE = "k3d_values.yaml"
    private static String K3D_BLUEPRINT_FILE = "k3d_blueprint.yaml"
    private static String YQ_VERSION = "4.40.5"
    // need to be installed before apply values.yaml
    private static String VERSION_ECOSYSTEM_CORE; // e.g.  "1.2.0"
    private static String VERSION_K8S_COMPONENT_OPERATOR_CRD; // e.g.  "1.10.1"
    // configured by values.yaml
    private static String VERSION_K8S_DOGU_OPERATOR; // e.g.  "3.15.0"
    private static String VERSION_K8S_DOGU_OPERATOR_CRD; // e.g.  "2.10.0"
    private static String VERSION_K8S_BLUEPRINT_OPERATOR; // e.g.  "3.0.2"
    private static String VERSION_K8S_BLUEPRINT_OPERATOR_CRD ; // e.g. "3.1.0"

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

    def defaultSetupConfig = [
        adminUsername          : "ces-admin",
        adminPassword          : "ecosystem2016",
        adminGroup             : "CesAdministrators",
        dependencies           : ["official/ldap",
                                  "official/cas",
                                  "official/postfix",
                                  "official/usermgt"],
        defaultDogu            : "",
        additionalDependencies : [],
        registryConfig         : "",
        registryConfigEncrypted: "",
        "enableBackup"         : false,
        "enableMonitoring"     : false
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
            " --k3s-arg=--disable=traefik@all:* " +
            // Disable servicelb (avoids "Pending" svclb pods and we use nodePorts right now anyway)
            " --k3s-arg=--disable=servicelb@all:* " +
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


    static void setVersionEcosystemCore(String v) {
        VERSION_ECOSYSTEM_CORE = v;
    }
    static void setVersionComponentOperatorCrd(String v) {
        VERSION_K8S_COMPONENT_OPERATOR_CRD = v;
    }
    static void setVersionDoguOperator(String v) {
        VERSION_K8S_DOGU_OPERATOR = v;
    }
    static void setVersionDoguOperatorCrd(String v) {
        VERSION_K8S_DOGU_OPERATOR_CRD = v;
    }
    static void setVersionBlueprintOperator(String v) {
        VERSION_K8S_BLUEPRINT_OPERATOR = v;
    }
    static void setVersionBlueprintOperatorCrd(String v) {
        VERSION_K8S_BLUEPRINT_OPERATOR_CRD = v;
    }

    void configureEcosystemCoreValues(config = [:]) {
        // Merge default config with the one passed as parameter
        config = defaultSetupConfig << config

        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".defaultConfig.env.waitTimeoutMinutes = 5")

        if (VERSION_K8S_DOGU_OPERATOR_CRD != null) {
            appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-dogu-operator-crd.version", VERSION_K8S_DOGU_OPERATOR_CRD)
        }
        if (VERSION_K8S_DOGU_OPERATOR != null) {
            appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-dogu-operator.version", VERSION_K8S_DOGU_OPERATOR)
        }
        if (VERSION_K8S_BLUEPRINT_OPERATOR_CRD != null) {
            appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-blueprint-operator-crd.version", VERSION_K8S_BLUEPRINT_OPERATOR_CRD)
        }
        if (VERSION_K8S_BLUEPRINT_OPERATOR != null) {
            appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-blueprint-operator-crd.version", VERSION_K8S_BLUEPRINT_OPERATOR)
        }

        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-blueprint-operator.version", VERSION_K8S_BLUEPRINT_OPERATOR)

        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-ces-control.disabled = true")

        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-service-discovery.valuesObject.loadBalancerService.internalTrafficPolicy", "Cluster")
        appendToYamlFile(K3D_VALUES_YAML_FILE, ".components.k8s-service-discovery.valuesObject.loadBalancerService.externalTrafficPolicy", "Cluster")

        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".backup.enabled = ${config.enableBackup}")
        yqEvalYamlFile(K3D_VALUES_YAML_FILE, ".monitoring.enabled = ${config.enableMonitoring}")

        script.echo "configuring ecosystem core..."
        writeBlueprintYaml(config)
    }

    @Deprecated
    void configureSetupJson(config = [:]) {
        configureEcosystemCoreValues(config)
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

    void installAndTriggerSetup(String tag, Integer timeout = 300, Integer interval = 5) {
        script.echo "Installing setup..."
        String registryUrl = "registry.cloudogu.com"
        String registryNamespace = "k8s"
        script.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'harborhelmchartpush', usernameVariable: 'HARBOR_USERNAME', passwordVariable: 'HARBOR_PASSWORD']]) {
            helm("registry login ${registryUrl} --username '${script.env.HARBOR_USERNAME}' --password '${script.env.HARBOR_PASSWORD}'")
        }

        // install crd first
        String comp_crd_version = VERSION_K8S_COMPONENT_OPERATOR_CRD == null ? "" : " --version ${VERSION_K8S_COMPONENT_OPERATOR_CRD}"
        helm("install k8s-component-operator-crd oci://${registryUrl}/${registryNamespace}/k8s-component-operator-crd  ${comp_crd_version} --namespace default")

        kubectl("--namespace default create configmap global-config --from-literal=config.yaml='fqdn: ${externalIP}'")

        String eco_core_version = VERSION_ECOSYSTEM_CORE == null ? "" : " --version ${VERSION_ECOSYSTEM_CORE}"
        helm("install -f ${K3D_VALUES_YAML_FILE} ecosystem-core oci://${registryUrl}/${registryNamespace}/ecosystem-core ${eco_core_version} --namespace default --timeout 15m")

        script.echo "Wait for blueprint-operator to be ready..."
        waitForDeploymentRollout("k8s-blueprint-operator-controller-manager", timeout, interval)

        kubectl("apply -f ${K3D_BLUEPRINT_FILE} --namespace default")

        script.echo "Wait for setup-finisher to be executed..."
        waitForSetupToFinish(timeout, interval)

        script.echo "Wait for dogus to be ready..."
        waitForDogusToBeRolledOut(timeout, interval)

        helm("registry logout ${registryUrl}")
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
            script.echo blueprintReady
            if (blueprintReady == "True True") {
                return
            }
        }

        this.script.error "failed to wait for ecosystem-core setup to finish: timeout"
    }

/**
 * Installs the ecosystem-core-setup to the cluster. Creates an example values.yaml and a blueprint-file with usermgt as dogu and executes the ecosystem-core-setup.
 * After that the method will wait until the blueprint is ready.
 * @param tag Tag of ecosystem-core e. g. "v1.4.0"
 * @param timout Timeout in seconds for the installation process e. g. 300
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
            String version;
            // "latest" needs to be replaced with actual last version
            if (parts.length != 2 || parts[1] == "latest") {
                version = this.getLatestVersion(parts[0])
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

    private String getLatestVersion(String doguName) {
        String tags = "{}";
        script.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: this.backendCredentialsID, usernameVariable: 'TOKEN_ID', passwordVariable: 'TOKEN_SECRET']]) {
            tags = this.sh.returnStdOut("curl https://registry.cloudogu.com/v2/${doguName}/tags/list -u ${script.env.TOKEN_ID}:${script.env.TOKEN_SECRET}").trim()
        }
        def obj = new JsonSlurper().parseText(tags)
        return obj.tags.max { t -> parseTag("${t}") }
    }

    private String parseTag(String tag) {
        def m = (tag =~ /^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-(\d+))?$/)
        if (!m.matches()) {
            // Fallback: set all to 0 to ingnore invalid tags
            return "00000.00000.00000.00000"
        }
        def major = (m[0][1] ?: "0") as int
        def minor = (m[0][2] ?: "0") as int
        def patch = (m[0][3] ?: "0") as int
        def build = (m[0][4] ?: "0") as int

        // Zero-padding → lexicographically sortable
        return sprintf("%05d.%05d.%05d.%05d", major, minor, patch, build)
    }

    private void writeBlueprintYaml(config) {
        List<String> deps = config.dependencies + config.additionalDependencies
        String formattedDeps = formatDependencies(deps)
        script.writeFile file: K3D_BLUEPRINT_FILE, text: """
apiVersion: k8s.cloudogu.com/v3
kind: Blueprint
metadata:
  labels:
    app: ces
    app.kubernetes.io/name: k8s-blueprint-lib
  name: blueprint-ces-module
  namespace: default
spec:
  displayName: "Blueprint K3D CES-Module"
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
        script.archiveArtifacts(artifacts: K3D_BLUEPRINT_FILE)
        script.sh("rm -rf ${K3D_BLUEPRINT_FILE}".toString())
        script.archiveArtifacts(artifacts: K3D_VALUES_YAML_FILE)
        script.sh("rm -rf ${K3D_VALUES_YAML_FILE}".toString())

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
