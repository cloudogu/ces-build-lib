package com.cloudogu.ces.cesbuildlib

import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class K3dTest extends GroovyTestCase {
    void testCreateClusterName() {
        K3d sut = new K3d("script", "leWorkSpace", "leK3dWorkSpace", "path")
        String testClusterName = sut.createClusterName()
        assertTrue(testClusterName.contains("citest-"))
        assertTrue(testClusterName != "citest-")
        assertTrue(testClusterName.length() <= 32)
        String testClusterName2 = sut.createClusterName()
        assertTrue(testClusterName != testClusterName2)
    }

    void testDeleteK3d() {
        // given
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsocconfig kubectl get nodeskname()[1]); s.close()\');'.toString(), "54321")
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, "leWorkSpace", "leK3dWorkSpace", "path")

        // we need to create a registry so that the deletion of the registry is triggered
        sut.startK3d()

        // when
        sut.deleteK3d()

        // then
        assertThat(scriptMock.allActualArgs[14].trim()).contains("k3d registry delete citest-")
        assertThat(scriptMock.allActualArgs[15].trim()).contains("k3d cluster delete citest-")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(16)
    }

    void testKubectl() {
        // given
        String workspaceDir = "leWorkspace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, "leK3dWorkSpace", "path")

        // when
        sut.kubectl("get nodes")

        // then
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get nodes".trim())
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(1)
    }

    void testStartK3d() {
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def k3dVer = "4.4.7"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        sut.startK3d()

        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("rm -rf ${k3dWorkspaceDir}/.k3d".toString())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("mkdir -p ${k3dWorkspaceDir}/.k3d/bin".toString())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${k3dVer} K3D_INSTALL_DIR=${k3dWorkspaceDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("echo -n \$(python3 -c 'import socket; s=socket.socket(); s.bind((\"\", 0)); print(s.getsockname()[1]); s.close()');")
        assertThat(scriptMock.allActualArgs[4].trim()).matches("k3d registry create citest-[0-9a-f]+ --port 54321")
        assertThat(scriptMock.allActualArgs[5].trim()).startsWith("k3d cluster create citest-")
        assertThat(scriptMock.allActualArgs[6].trim()).startsWith("k3d kubeconfig merge citest-")
        assertThat(scriptMock.allActualArgs[7].trim()).startsWith("snap list kubectl")
        assertThat(scriptMock.allActualArgs[8].trim()).startsWith("sudo snap install kubectl")
        assertThat(scriptMock.allActualArgs[9].trim()).startsWith("echo \"Using credentials: cesmarvin-setup\"")
        assertThat(scriptMock.allActualArgs[10].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-dogu-registry || true")
        assertThat(scriptMock.allActualArgs[11].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-docker-registry || true")
        assertThat(scriptMock.allActualArgs[12].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"null\" --from-literal=password=\"null\"")
        assertThat(scriptMock.allActualArgs[13].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret docker-registry k8s-dogu-operator-docker-registry --docker-server=\"registry.cloudogu.com\" --docker-username=\"null\" --docker-email=\"a@b.c\" --docker-password=\"null\"")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(14)
    }

    void testStartK3dWithCustomCredentials() {
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "path"
        def k3dVer = "4.4.7"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "", "myBackendCredentialsID")

        sut.startK3d()

        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("rm -rf ${k3dWorkspaceDir}/.k3d".toString())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("mkdir -p ${k3dWorkspaceDir}/.k3d/bin".toString())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${k3dVer} K3D_INSTALL_DIR=${k3dWorkspaceDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("echo -n \$(python3 -c 'import socket; s=socket.socket(); s.bind((\"\", 0)); print(s.getsockname()[1]); s.close()');")
        assertThat(scriptMock.allActualArgs[4].trim()).matches("k3d registry create citest-[0-9a-f]+ --port 54321")
        assertThat(scriptMock.allActualArgs[5].trim()).startsWith("k3d cluster create citest-")
        assertThat(scriptMock.allActualArgs[6].trim()).startsWith("k3d kubeconfig merge citest-")
        assertThat(scriptMock.allActualArgs[7].trim()).startsWith("snap list kubectl")
        assertThat(scriptMock.allActualArgs[8].trim()).startsWith("sudo snap install kubectl")
        assertThat(scriptMock.allActualArgs[9].trim()).startsWith("echo \"Using credentials: myBackendCredentialsID\"")
        assertThat(scriptMock.allActualArgs[10].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-dogu-registry || true")
        assertThat(scriptMock.allActualArgs[11].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-docker-registry || true")
        assertThat(scriptMock.allActualArgs[12].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"null\" --from-literal=password=\"null\"")
        assertThat(scriptMock.allActualArgs[13].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret docker-registry k8s-dogu-operator-docker-registry --docker-server=\"registry.cloudogu.com\" --docker-username=\"null\" --docker-email=\"a@b.c\" --docker-password=\"null\"")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(14)
    }

    void testBuildAndPush() {
        // given
        String imageName = "superimage"
        String imageTag = "1.2.1"

        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        def scriptMock = new ScriptMock(dockerMock)

        when(dockerMock.build(anyString())).thenReturn(imageMock)
        when(dockerMock.withRegistry(anyString(), Mockito.any(Closure.class))).then(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                Closure doThings = (Closure) invocation.getArgument(1)
                doThings.call()
            }
        })
        when(imageMock.push(imageTag)).then(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                scriptMock.sh("image pushed")
            }
        })
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, "leWorkSpace", "leK3dWorkSpace", "path")
        sut.startK3d()

        // when
        sut.buildAndPushToLocalRegistry(imageName, imageTag)

        // then
        assertThat(scriptMock.allActualArgs[14].trim()).isEqualTo("image pushed".toString())
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(15)
    }

    void testSetup() {
        // given
        def workspaceEnvDir = "leK3dWorkSpace"
        String tag = "v0.6.0"
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("curl -s https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup.yaml".toString(), "fake setup yaml with {{ .Namespace }}")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/k8s-dogu-operator-controller-manager".toString(), "successfully rolled out")
        scriptMock.expectedShRetValueForScript.put("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip", "192.168.56.2")

        K3d sut = new K3d(scriptMock, "leWorkSpace", "leK3dWorkSpace", "path")

        // when
        sut.setup(tag, [:], 1, 1)

        // then
        assertThat(scriptMock.actualEcho.get(0)).isEqualTo("configuring setup...")
        assertThat(scriptMock.actualEcho.get(1)).isEqualTo("Installing setup...")
        assertThat(scriptMock.actualEcho.get(2)).isEqualTo("Wait for dogu-operator to be ready...")

        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip")
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl apply -f https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup-config.yaml".trim())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl create configmap k8s-ces-setup-json --from-file=setup.json".trim())
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup.yaml".trim())
        assertThat(scriptMock.allActualArgs[4].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl apply -f setup.yaml".trim())
        assertThat(scriptMock.allActualArgs[5].trim()).isEqualTo("sleep 1s")
        assertThat(scriptMock.allActualArgs[6].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/k8s-dogu-operator-controller-manager".trim())

        assertThat(scriptMock.writeFileParams.get(0)).isNotNull()
        String setupYaml = scriptMock.writeFileParams.get(1)
        assertThat(setupYaml).isNotNull()
        assertThat(setupYaml.contains("{{ .Namespace }}")).isFalse()
    }

    void testSetupShouldThrowExceptionOnDoguOperatorRollout() {
        // given
        def workspaceEnvDir = "leK3dWorkSpace"
        String tag = "v0.6.0"
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("curl -s https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup.yaml".toString(), "fake setup yaml with {{ .Namespace }}")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/k8s-dogu-operator-controller-manager".toString(), "error rollout")
        scriptMock.expectedShRetValueForScript.put("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip", "192.168.56.2")


        K3d sut = new K3d(scriptMock, "leWorkSpace", "leK3dWorkSpace", "path")

        // when
        shouldFail(RuntimeException) {
            sut.setup(tag, [:], 1, 1)
        }

        // then
        assertThat(scriptMock.actualEcho.get(0)).isEqualTo("configuring setup...")
        assertThat(scriptMock.actualEcho.get(1)).isEqualTo("Installing setup...")
        assertThat(scriptMock.actualEcho.get(2)).isEqualTo("Wait for dogu-operator to be ready...")

        assertThat(scriptMock.actualShMapArgs[0].trim()).isEqualTo("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip")
        assertThat(scriptMock.actualShMapArgs[1].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl apply -f https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup-config.yaml".trim())
        assertThat(scriptMock.writeFileParams.get(0)).isNotNull()
        assertThat(scriptMock.actualShMapArgs[2].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl create configmap k8s-ces-setup-json --from-file=setup.json".trim())
        assertThat(scriptMock.actualShMapArgs[3].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/cloudogu/k8s-ces-setup/${tag}/k8s/k8s-ces-setup.yaml".trim())
        String setupYaml = scriptMock.writeFileParams.get(1)
        assertThat(setupYaml).isNotNull()
        assertThat(setupYaml.contains("{{ .Namespace }}")).isFalse()
        assertThat(scriptMock.actualShMapArgs[4].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl apply -f setup.yaml".trim())
        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("sleep 1s")
        assertThat(scriptMock.actualShMapArgs[5].trim()).isEqualTo("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/k8s-dogu-operator-controller-manager".trim())
    }

    void testK3d_installDogu() {
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"

        String exampleDockerInspect = """
[
  {
    "HostConfig": {
      "PortBindings": {
        "5000/tcp": [
          {
            "HostIp": "0.0.0.0",
            "HostPort": "35419"
          }
        ]
      },
    "Config": {
      "Hostname": "k3d-citest-c6cc60667cf3",
      "Labels": {
        "k3s.registry.port.internal": "5000"
      }
    },
    "NetworkSettings": {
      "Networks": {
        "k3d-citest-c6cc60667cf3": {
          "IPAMConfig": null,
          "Links": null,
          "Aliases": [
            "89309458869a",
            "k3d-citest-c6cc60667cf3"
          ],
          "NetworkID": "34aa9caac510f0ba78e3f18aa8e5c5d7e1d915ee4404524f93a95b2ee3281254",
          "EndpointID": "961364ea3a160c66472d5618c5842e2e4f80f27dbd795eea0f32b3442b696b66",
          "Gateway": "192.168.32.1",
          "IPAddress": "192.168.32.2",
          "IPPrefixLen": 20,
          "IPv6Gateway": "",
          "GlobalIPv6Address": "",
          "GlobalIPv6PrefixLen": 0,
          "MacAddress": "02:42:c0:a8:20:02",
          "DriverOpts": null
        }
      }
    }
  }
]
"""

        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")
        String prefixedRegistryName = "k3d-${sut.getRegistryName()}"
        String port = "5000"

        scriptMock.expectedShRetValueForScript.put('whoami'.toString(), "itsme")
        scriptMock.expectedShRetValueForScript.put('cat /etc/passwd | grep itsme'.toString(), "test:x:900:1001::/home/test:/bin/sh")
        scriptMock.expectedShRetValueForScript.put("docker inspect " + prefixedRegistryName, exampleDockerInspect)
        scriptMock.expectedShRetValueForScript.put("echo '" + exampleDockerInspect + "' | yq '.[].NetworkSettings.Networks." + prefixedRegistryName + ".IPAddress'", "192.168.32.2")
        scriptMock.expectedShRetValueForScript.put("echo '" + exampleDockerInspect + "' | yq '.[].Config.Labels.\"k3s.registry.port.internal\"'", port)
        scriptMock.expectedShRetValueForScript.put("yq -e '.Image' dogu.json | sed 's|registry\\.cloudogu\\.com\\(.\\+\\)|" + "myIP" + ".local:" + port + "\\1|g'", "myIP.local:5000/test/myimage:0.1.2")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get pod --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", "test-execpod")
        scriptMock.expectedShRetValueForScript.put("echo 'test-execpod' | grep 'test-execpod'", "test-execpod")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get deployment --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", "test")
        scriptMock.expectedShRetValueForScript.put("echo 'test' | grep 'test'", "test")

        String doguYaml = """
apiVersion: k8s.cloudogu.com/v1
kind: Dogu
metadata:
  name: nginx-ingress
  labels:
    dogu: nginx-ingress
spec:
  name: k8s/nginx-ingress
  version: 1.1.2-1-0
"""

        sut.installDogu("test", "myIP:1234/test/myimage:0.1.2", doguYaml)

        assertThat(scriptMock.allActualArgs[0].trim()).contains("docker inspect k3d-citest-")
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("whoami")
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("cat /etc/passwd | grep itsme")
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("echo '" + exampleDockerInspect + "' | yq '.[].NetworkSettings.Networks." + prefixedRegistryName + ".IPAddress'")
        assertThat(scriptMock.allActualArgs[4].trim()).isEqualTo("echo '" + exampleDockerInspect + "' | yq '.[].Config.Labels.\"k3s.registry.port.internal\"'")
        assertThat(scriptMock.allActualArgs[5].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl -n kube-system patch cm coredns --patch-file coreDNSPatch.yaml")
        assertThat(scriptMock.allActualArgs[6].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl rollout restart -n kube-system deployment/coredns")
        assertThat(scriptMock.allActualArgs[7].trim()).isEqualTo("whoami")
        assertThat(scriptMock.allActualArgs[8].trim()).isEqualTo("cat /etc/passwd | grep itsme")
        assertThat(scriptMock.allActualArgs[9].trim()).isEqualTo("yq -e '.Image' dogu.json | sed 's|registry\\.cloudogu\\.com\\(.\\+\\)|myIP.local:5000\\1|g'")
        assertThat(scriptMock.allActualArgs[10].trim()).isEqualTo("yq '.Image=\"myIP.local:5000/test/myimage:0.1.2\"' dogu.json > leWorkspace/target/dogu.json")
        assertThat(scriptMock.allActualArgs[11].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl create configmap test-descriptor --from-file=leWorkspace/target/dogu.json")
        assertThat(scriptMock.allActualArgs[12].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl apply -f \n" +
            "apiVersion: k8s.cloudogu.com/v1\n" +
            "kind: Dogu\n" +
            "metadata:\n" +
            "  name: nginx-ingress\n" +
            "  labels:\n" +
            "    dogu: nginx-ingress\n" +
            "spec:\n" +
            "  name: k8s/nginx-ingress\n" +
            "  version: 1.1.2-1-0")
        assertThat(scriptMock.allActualArgs[13].trim()).isEqualTo("sleep 2s")
        assertThat(scriptMock.allActualArgs[14].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get pod --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'")
        assertThat(scriptMock.allActualArgs[15].trim()).isEqualTo("echo 'test-execpod' | grep 'test-execpod'")
        assertThat(scriptMock.allActualArgs[16].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl patch pod 'test-execpod' -p '{\"spec\":{\"containers\":[{\"name\":\"test-execpod\",\"image\":\"myIP:1234/test/myimage:0.1.2\"}]}}'")
        assertThat(scriptMock.allActualArgs[17].trim()).isEqualTo("sleep 5s")
        assertThat(scriptMock.allActualArgs[18].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get deployment --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'")
        assertThat(scriptMock.allActualArgs[19].trim()).isEqualTo("echo 'test' | grep 'test'")
        assertThat(scriptMock.allActualArgs[20].trim()).startsWith("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl patch deployment 'test' -p '{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"test\",\"image\":\"myIP:1234/test/myimage:0.1.2\"}]}}}}'")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(21)
    }


    void testK3d_collectAndArchiveLogs() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        def relevantResources = ["persistentvolumeclaim","statefulset","replicaset","deployment","service","secret","pod"]
        for(def resource : relevantResources) {
            scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get ${resource} --show-kind --ignore-not-found -l app=ces -o yaml || true".toString(), "value for ${resource}")
            scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe ${resource} -l app=ces || true".toString(), "value for ${resource}")
        }

        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get dogu --ignore-not-found -o name || true".toString(), "k8s.cloudogu.com/testdogu")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe k8s.cloudogu.com/testdogu || true".toString(), "this is the description of a dogu")

        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get pods -o name || true".toString(), "pod/testpod-1234\npod/testpod2-1234")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl logs pod/testpod-1234 || true".toString(), "this is the log from testpod")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl logs pod/testpod2-1234 || true".toString(), "this is the log from testpod2")

        // when
        sut.collectAndArchiveLogs()

        // then
        int i = 0
        int fileCounter = 0
        assertThat(scriptMock.allActualArgs[i++].trim()).contains("called deleteDir()")
        assertThat(scriptMock.allActualArgs[i++].trim()).contains("rm -rf k8sLogs.zip")

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get persistentvolumeclaim --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "persistentvolumeclaim.yaml", "text": "value for persistentvolumeclaim"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe persistentvolumeclaim -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "persistentvolumeclaim_description.yaml", "text": "value for persistentvolumeclaim"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get statefulset --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "statefulset.yaml", "text": "value for statefulset"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe statefulset -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "statefulset_description.yaml", "text": "value for statefulset"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get replicaset --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "replicaset.yaml", "text": "value for replicaset"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe replicaset -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "replicaset_description.yaml", "text": "value for replicaset"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get deployment --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "deployment.yaml", "text": "value for deployment"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe deployment -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "deployment_description.yaml", "text": "value for deployment"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get service --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "service.yaml", "text": "value for service"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe service -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "service_description.yaml", "text": "value for service"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get secret --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "secret.yaml", "text": "value for secret"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe secret -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "secret_description.yaml", "text": "value for secret"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get pod --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "pod.yaml", "text": "value for pod"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe pod -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "pod_description.yaml", "text": "value for pod"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get dogu --ignore-not-found -o name || true")
        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe k8s.cloudogu.com/testdogu || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "testdogu.txt", "text": "this is the description of a dogu"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get pods -o name || true")
        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl logs pod/testpod-1234 || true")
        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl logs pod/testpod2-1234 || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "testpod-1234.log", "text": "this is the log from testpod"])
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "testpod2-1234.log", "text": "this is the log from testpod2"])

        assertThat(scriptMock.zipParams.size()).isEqualTo(1)
        assertThat(scriptMock.zipParams[0]).isEqualTo(["archive":"false", "dir":"k8sLogs", "zipFile":"k8sLogs.zip"])
        assertThat(scriptMock.archivedArtifacts.size()).isEqualTo(1)
        assertThat(scriptMock.archivedArtifacts[0]).isEqualTo(["allowEmptyArchive":"true", "artifacts":"k8sLogs.zip"])

        assertThat(scriptMock.allActualArgs.size()).isEqualTo(i)
        assertThat(scriptMock.writeFileParams.size()).isEqualTo(17)
        assertThat(fileCounter).isEqualTo(17)
    }

    void testK3d_applyDoguResource() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        def filename = "target/make/k8s/testName.yaml"
        def doguContentYaml = """
apiVersion: k8s.cloudogu.com/v1
kind: Dogu
metadata:
  name: testName
  labels:
    dogu: testName
spec:
  name: nyNamespace/testName
  version: 14.1.1-1
"""

        // when
        sut.applyDoguResource("testName", "nyNamespace", "14.1.1-1")

        // then
        assertThat(scriptMock.writeFileParams[0]).isEqualTo(["file": filename, "text": doguContentYaml])
        assertThat(scriptMock.writeFileParams.size()).isEqualTo(1)

        assertThat(scriptMock.allActualArgs[0].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl apply -f target/make/k8s/testName.yaml")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(1)
    }
}
