package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS

class K3d {
    /**
     * The image of the k3s version defining the targeted k8s version
     */
    private static String K8S_IMAGE = "rancher/k3s:v1.21.2-k3s1"
    /**
     * The version of k3d to be installed
     */
    private static String K3D_VERSION = "4.4.7"

    private String clusterName
    private script
    private String path
    private String k3dDir
    private String k3dBinaryDir
    private String backendCredentialsID
    private Sh sh
    private K3dRegistry registry
    public String registryName

    /**
     * Create an object to set up, modify and tear down a local k3d cluster
     *
     * @param script The Jenkins script you are coming from (aka "this")
     * @param envWorkspace The designated directory for the K3d installation
     * @param envPath The PATH environment variable; in Jenkins use "env.PATH" for example
     * @param backendCredentialsID Identifier of credentials used to log into the backend. Default: cesmarvin-setup
     */
    K3d(script, String envWorkspace, String envPath, String backendCredentialsID="cesmarvin-setup") {
        this.clusterName = createClusterName()
        this.registryName = clusterName
        this.script = script
        this.path = envPath
        this.k3dDir = "${envWorkspace}/.k3d"
        this.k3dBinaryDir = "${k3dDir}/bin"
        this.backendCredentialsID = backendCredentialsID
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
        // Make k3d write kubeconfig to WORKSPACE
        // Install k3d binary to workspace in order to avoid concurrency issues
        script.withEnv(["HOME=${k3dDir}", "PATH=${k3dBinaryDir}:${path}"]) {
            installK3d()
            installLocalRegistry()
            initializeCluster()
            installKubectl()
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
            kubectl("delete secret k8s-dogu-operator-docker-registry || true")

            //create secret for the backend registry
            kubectl("create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"${script.env.TOKEN_ID}\" --from-literal=password=\"${script.env.TOKEN_SECRET}\"")
            kubectl("create secret docker-registry k8s-dogu-operator-docker-registry --docker-server=\"registry.cloudogu.com\" --docker-username=\"${script.env.TOKEN_ID}\" --docker-email=\"a@b.c\" --docker-password=\"${script.env.TOKEN_SECRET}\"")
        }
    }

    /**
     * Initializes the cluster by creating a respective cluster in k3d.
     */
    private void initializeCluster() {
        script.sh "k3d cluster create ${clusterName} " +
            // Allow services to bind to ports < 30000
            " --k3s-server-arg=--kube-apiserver-arg=service-node-port-range=8010-32767 " +
            // Used by Jenkins Agents pods
            " -v /var/run/docker.sock:/var/run/docker.sock@server[0] " +
            // Allows for finding out the GID of the docker group in order to allow the Jenkins agents pod to access docker socket
            " -v /etc/group:/etc/group@server[0] " +
            // Persists the cache of Jenkins agents pods for faster builds
            " -v /tmp:/tmp@server[0] " +
            // Disable traefik (no ingresses used so far)
            " --k3s-server-arg=--disable=traefik " +
            // Disable servicelb (avoids "Pending" svclb pods and we use nodePorts right now anyway)
            " --k3s-server-arg=--disable=servicelb " +
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
     * Installs the setup to the cluster. Creates an example setup.json with plantuml as dogu and executes the setup.
     * After that the method will wait until the dogu-operator is ready.
     * @param tag Tag of the setup e. g. "v0.6.0"
     */
    void setup(String tag) {
        // Config
        script.echo "Installing setup..."
        kubectl("apply -f https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup-config.yaml")
        writeSetupJson()
        kubectl('create configmap k8s-ces-setup-json --from-file=setup.json')

        String setup = script.sh(script: "curl -s https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup.yaml", returnStdout: true)
        setup = setup.replace("{{ .Namespace }}", "default")
        script.writeFile file: 'setup.yaml', text: setup
        kubectl('apply -f setup.yaml')

        script.sh "Wait for dogu-operator to be ready..."
        waitForDeploymentRollout("k8s-dogu-operator-controller-manager", 300, 5)
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
        Docker docker = new Docker(this)
        String[] IpPort = getRegistryIpAndPort(docker)
        String imageUrl = image.split(":")[0]
        patchCoreDNS(IpPort[0], imageUrl)

        applyDevDoguDescriptor(docker, dogu, imageUrl, IpPort[1])
        kubectl("apply -f ${doguYaml}")

        // Remove .local from Images.
        patchDoguExecPod(dogu, image)
        patchDoguDeployment(dogu, image)
    }

    private void applyDevDoguDescriptor(Docker docker, String dogu, String imageUrl, String port) {
        String imageDev
        String doguJsonDevFile = "${WORKSPACE}/target/dogu.json"
        docker.image('mikefarah/yq:4.22.1')
            .mountJenkinsUser()
            .inside("--volume ${WORKSPACE}:/workdir -w /workdir") {
                imageDev = script.sh(script: "yq -e '.Image' dogu.json | sed 's|registry\\.cloudogu\\.com\\(.\\+\\)|${imageUrl}.local:${port}\\1|g'", returnStdout: true)
                script.sh "yq '.Image=\"${imageDev}\"' dogu.json > ${doguJsonDevFile}"
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

    private String getExecPodName(String dogu, Integer timeout, Integer interval) {
        String execPodName
        for (int i = 0; i < timeout/interval; i++) {
            sleep(time: interval, unit: "SECONDS")
            try {
                execPodName = script.sh(script: "kubectl get pod --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}' | grep '${dogu}-execpod'", returnStdout: true).trim()
                if (execPodName.contains("${dogu}-execpod")) {
                    return execPodName
                }
            } catch (exception) {
                // Ignore Error.
            }
        }

        // Don't throw an error here because in the future the won't be exec pods for every dogu
        return ""
    }

    void waitForDeployment(String deployment, Integer timeout, Integer interval) {
        for (int i = 0; i < timeout/interval; i++) {
            sleep(time: interval, unit: "SECONDS")
            try {
                String statusMsg = script.sh(script: "kubectl get deployment --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}' | grep '${deployment}'", returnStdout: true)
                if (statusMsg == deployment) {
                    return
                }
            } catch (ignored) {
                // Ignore Error.
            }
        }

        throw new Exception("failed to wait for deployment/${deployment}: timeout")
    }

    void waitForDeploymentRollout(String deployment, Integer timeout, Integer interval) {
        for (int i = 0; i < timeout/interval; i++) {
            sleep(time: interval, unit: "SECONDS")
            try {
                String statusMsg = kubectl("rollout status deployment/${deployment}", true)
                if (statusMsg.contains("successfully rolled out")) {
                    return
                }
            } catch (ignored) {
                // If the deployment does not even exists an error will be thrown. Ignore this case.
            }
        }

        throw new Exception("failed to wait for deployment/${deployment} rollout: timeout")
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

    private String[] getRegistryIpAndPort(Docker docker) {
        String registryIp
        String registryPortProtocol
        String prefixedRegistryName = "k3d-${this.registryName}"
        String dockerInspect = script.sh(script: "docker inspect ${prefixedRegistryName}", returnStdout: true)
        docker.image('mikefarah/yq:4.22.1')
            .mountJenkinsUser().inside("--volume ${WORKSPACE}:/workdir -w /workdir") {
                registryIp = script.sh(script: "echo '${dockerInspect}' | yq '.[].NetworkSettings.Networks.${prefixedRegistryName}.IPAddress'", returnStdout: true).trim()
                registryPortProtocol = script.sh(script: "echo '${dockerInspect}' | yq '.[].Config.Labels.\"k3s.registry.port.internal\"'", returnStdout: true).trim()
            }
        String registryPort = registryPortProtocol.split("/")[0]

        return [registryIp, registryPort]
    }

    private void writeSetupJson() {
        script.writeFile file: 'setup.json', text: """
{
  "naming": {
    "fqdn": "192.168.56.2",
    "domain": "k3ces.local",
    "certificateType": "selfsigned",
    "relayHost": "asdf",
    "completed": true,
    "useInternalIp": false,
    "internalIp": ""
  },
  "dogus": {
    "defaultDogu": "plantuml",
    "install": [
      "official/plantuml"
    ],
    "completed": true
  },
  "admin": {
    "username": "admin",
    "mail": "admin@admin.admin",
    "password": "adminpw",
    "adminGroup": "cesAdmin",
    "completed": true,
    "adminMember": true,
    "sendWelcomeMail": false
  },
  "userBackend": {
    "dsType": "embedded",
    "server": "",
    "attributeID": "uid",
    "attributeGivenName": "",
    "attributeSurname": "",
    "attributeFullname": "cn",
    "attributeMail": "mail",
    "attributeGroup": "memberOf",
    "baseDN": "",
    "searchFilter": "(objectClass=person)",
    "connectionDN": "",
    "password": "",
    "host": "ldap",
    "port": "389",
    "loginID": "",
    "loginPassword": "",
    "encryption": "",
    "completed": true,
    "groupBaseDN": "",
    "groupSearchFilter": "",
    "groupAttributeName": "",
    "groupAttributeDescription": "",
    "groupAttributeMember": ""
  }
}
"""
    }
}
