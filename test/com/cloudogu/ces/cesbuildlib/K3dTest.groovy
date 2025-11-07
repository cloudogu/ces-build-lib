package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

import static org.junit.jupiter.api.Assertions.*
import static groovy.test.GroovyAssert.shouldFail

class K3dTest {

    @Test
    void testCreateClusterName() {
        K3d sut = new K3d("script", "leWorkSpace", "leK3dWorkSpace", "path")
        String testClusterName = sut.createClusterName()
        assertTrue(testClusterName.contains("citest-"))
        assertTrue(testClusterName != "citest-")
        assertTrue(testClusterName.length() <= 32)
        String testClusterName2 = sut.createClusterName()
        assertTrue(testClusterName != testClusterName2)
    }

    @Test
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
        assertThat(scriptMock.allActualArgs[22].trim()).contains("k3d registry delete citest-")
        assertThat(scriptMock.allActualArgs[23].trim()).contains("k3d cluster delete citest-")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(24)
    }

    @Test
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

    @Test
    void testHelm() {
        // given
        String workspaceDir = "leWorkspace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, "leK3dWorkSpace", "path")

        // when
        sut.helm("install path/to/chart/")

        // then
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config helm install path/to/chart/".trim())
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(1)
    }

    // we cannot test lazy-installation because the mock is incapable of mocking the right types, the right values
    // and thus repeated calls to the same script with different results.
    @Test
    void testInstallHelm_initially() {
        // given
        String workspaceDir = "leWorkspace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, "leK3dWorkSpace", "path")

        // when
        sut.installHelm()

        // then
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(2)
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("snap list helm".trim())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("sudo snap install helm --classic".trim())
    }

    @Test
    void testStartK3d() {
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def expectedK3dVer = "5.6.0"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        sut.startK3d()

        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("rm -rf ${k3dWorkspaceDir}/.k3d".toString())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("mkdir -p ${k3dWorkspaceDir}/.k3d/bin".toString())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${expectedK3dVer} K3D_INSTALL_DIR=${k3dWorkspaceDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("echo -n \$(python3 -c 'import socket; s=socket.socket(); s.bind((\"\", 0)); print(s.getsockname()[1]); s.close()');")
        assertThat(scriptMock.allActualArgs[4].trim()).matches("k3d registry create citest-[0-9a-f]+ --port 54321")
        assertThat(scriptMock.allActualArgs[5].trim()).startsWith("k3d cluster create citest-")
        assertThat(scriptMock.allActualArgs[6].trim()).startsWith("k3d kubeconfig merge citest-")
        assertThat(scriptMock.allActualArgs[7].trim()).startsWith("snap list kubectl")
        assertThat(scriptMock.allActualArgs[8].trim()).startsWith("sudo snap install kubectl --classic")
        assertThat(scriptMock.allActualArgs[9].trim()).startsWith("snap list helm")
        assertThat(scriptMock.allActualArgs[10].trim()).startsWith("sudo snap install helm --classic")
        assertThat(scriptMock.allActualArgs[11].trim()).startsWith("echo \"Using credentials: cesmarvin-setup\"")
        assertThat(scriptMock.allActualArgs[12].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-dogu-registry || true")
        assertThat(scriptMock.allActualArgs[13].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret ces-container-registries || true")
        assertThat(scriptMock.allActualArgs[14].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"null\" --from-literal=password=\"null\"")
        assertThat(scriptMock.allActualArgs[15].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret docker-registry ces-container-registries --docker-server=\"registry.cloudogu.com\" --docker-username=\"null\" --docker-email=\"a@b.c\" --docker-password=\"null\"")
        assertThat(scriptMock.allActualArgs[16].trim()).startsWith("echo \"Using credentials: harborhelmchartpush\"")
        assertThat(scriptMock.allActualArgs[17].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete configmap component-operator-helm-repository || true")
        assertThat(scriptMock.allActualArgs[18].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret component-operator-helm-registry || true")
        assertThat(scriptMock.allActualArgs[19].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create configmap component-operator-helm-repository --from-literal=endpoint=\"registry.cloudogu.com\" --from-literal=schema=\"oci\" --from-literal=plainHttp=\"false\"")
        assertThat(scriptMock.allActualArgs[20].trim()).startsWith("printf '%s:%s' 'null' 'null' | base64")
        assertThat(scriptMock.allActualArgs[21].trim()).startsWith("set +x; sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl create secret generic component-operator-helm-registry --from-literal=config.json='{\"auths\": {\"registry.cloudogu.com\": {\"auth\": \"null\"}}}'")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(22)
    }

    @Test
    void testStartK3dWithCustomCredentials() {
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "path"
        def expectedK3dVer = "5.6.0"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "", "myBackendCredentialsID", "myHarborCredentials")

        sut.startK3d()

        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("rm -rf ${k3dWorkspaceDir}/.k3d".toString())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("mkdir -p ${k3dWorkspaceDir}/.k3d/bin".toString())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${expectedK3dVer} K3D_INSTALL_DIR=${k3dWorkspaceDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("echo -n \$(python3 -c 'import socket; s=socket.socket(); s.bind((\"\", 0)); print(s.getsockname()[1]); s.close()');")
        assertThat(scriptMock.allActualArgs[4].trim()).matches("k3d registry create citest-[0-9a-f]+ --port 54321")
        assertThat(scriptMock.allActualArgs[5].trim()).startsWith("k3d cluster create citest-")
        assertThat(scriptMock.allActualArgs[6].trim()).startsWith("k3d kubeconfig merge citest-")
        assertThat(scriptMock.allActualArgs[7].trim()).startsWith("snap list kubectl")
        assertThat(scriptMock.allActualArgs[8].trim()).startsWith("sudo snap install kubectl")
        assertThat(scriptMock.allActualArgs[9].trim()).startsWith("snap list helm")
        assertThat(scriptMock.allActualArgs[10].trim()).startsWith("sudo snap install helm")
        assertThat(scriptMock.allActualArgs[11].trim()).startsWith("echo \"Using credentials: myBackendCredentialsID\"")
        assertThat(scriptMock.allActualArgs[12].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-dogu-registry || true")
        assertThat(scriptMock.allActualArgs[13].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret ces-container-registries || true")
        assertThat(scriptMock.allActualArgs[14].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"null\" --from-literal=password=\"null\"")
        assertThat(scriptMock.allActualArgs[15].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create secret docker-registry ces-container-registries --docker-server=\"registry.cloudogu.com\" --docker-username=\"null\" --docker-email=\"a@b.c\" --docker-password=\"null\"")
        assertThat(scriptMock.allActualArgs[16].trim()).startsWith("echo \"Using credentials: myHarborCredentials\"")
        assertThat(scriptMock.allActualArgs[17].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete configmap component-operator-helm-repository || true")
        assertThat(scriptMock.allActualArgs[18].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl delete secret component-operator-helm-registry || true")
        assertThat(scriptMock.allActualArgs[19].trim()).startsWith("sudo KUBECONFIG=${k3dWorkspaceDir}/.k3d/.kube/config kubectl create configmap component-operator-helm-repository --from-literal=endpoint=\"registry.cloudogu.com\" --from-literal=schema=\"oci\" --from-literal=plainHttp=\"false\"")
        assertThat(scriptMock.allActualArgs[20].trim()).startsWith("printf '%s:%s' 'null' 'null' | base64")
        assertThat(scriptMock.allActualArgs[21].trim()).startsWith("set +x; sudo KUBECONFIG=path/.k3d/.kube/config kubectl create secret generic component-operator-helm-registry --from-literal=config.json='{\"auths\": {\"registry.cloudogu.com\": {\"auth\": \"null\"}}}'")
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(22)
    }

    @Test
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
        assertThat(scriptMock.allActualArgs[22].trim()).isEqualTo("image pushed".toString())
        assertThat(scriptMock.allActualArgs.size()).isEqualTo(23)
    }

    @Test
    void testSetup() {
        // given
        def workspaceEnvDir = "leK3dWorkSpace"
        String tag = "v0.6.0"
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip", "192.168.56.2")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/ldap/tags/list -u null:null", "{\"tags\": [\"1.0.0\"]}")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/cas/tags/list -u null:null", "{\"tags\": [\"2.0.0\"]}")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/postfix/tags/list -u null:null", "{\"tags\": [\"3.0.0\"]}")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/usermgt/tags/list -u null:null", "{\"tags\": [\"4.0.0\"]}")


        scriptMock.expectedShRetValueForScript.put("whoami", "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        scriptMock.expectedShRetValueForScript.put("yq -i '.setup_json = load_str(\"k3d_setup.json\")' k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/k8s-dogu-operator-controller-manager".toString(), "successfully rolled out")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/k8s-blueprint-operator-controller-manager".toString(), "successfully rolled out")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl get blueprint -n=default blueprint-ces-module -o jsonpath='{.status.conditions[?(@.type==\"EcosystemHealthy\")].status}{\" \"}{.status.conditions[?(@.type==\"Completed\")].status}'".toString(), "True True")


        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl get deployments --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'".toString(), "k8s-dogu-operator\nk8s-service-discovery")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl get dogus --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'".toString(), "cas\nldap")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/cas".toString(), "successfully rolled out")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=${workspaceEnvDir}/.k3d/.kube/config kubectl rollout status deployment/ldap".toString(), "successfully rolled out")


        K3d sut = new K3d(scriptMock, "leWorkSpace", "leK3dWorkSpace", "path")

        // when
        sut.setup(tag, [:], 1, 1)

        // then
        for (int i = 0; i < 10; i++) {
            assertThat(scriptMock.actualEcho.get(i)).isEqualTo("create values.yaml for setup deployment")
        }
        assertThat(scriptMock.actualEcho.get(10)).isEqualTo("configuring ecosystem core...")
        assertThat(scriptMock.actualEcho.get(11)).isEqualTo("Installing setup...")
        assertThat(scriptMock.actualEcho.get(12)).isEqualTo("Wait for blueprint-operator to be ready...")
        assertThat(scriptMock.actualEcho.get(13)).isEqualTo("Wait for setup-finisher to be executed...")
        assertThat(scriptMock.actualEcho.get(14)).isEqualTo("True True")
        assertThat(scriptMock.actualEcho.get(15)).isEqualTo("Wait for dogus to be ready...")
        assertThat(scriptMock.actualEcho.get(16)).isEqualTo("Wait for cas to be rolled out...")
        assertThat(scriptMock.actualEcho.get(17)).isEqualTo("Wait for ldap to be rolled out...")

        assertThat(scriptMock.writeFileParams.get(0)).isNotNull()
        String setupYaml = scriptMock.writeFileParams.get(1)
        assertThat(setupYaml).isNotNull()
        assertThat(setupYaml.contains("{{ .Namespace }}")).isFalse()
    }

    @Test
    void testSetupShouldThrowExceptionOnDoguOperatorRollout() {
        // given
        String tag = "v0.6.0"
        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip", "192.168.56.2")
        scriptMock.expectedShRetValueForScript.put("whoami", "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        scriptMock.expectedShRetValueForScript.put("yq -i '.setup_json = load_str(\"k3d_setup.json\")' k3d_values.yaml", "fake")
        scriptMock.expectedShRetValueForScript.put("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip", "192.168.56.2")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/ldap/tags/list -u null:null", "{\"tags\": [\"1.0.0\"]}")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/cas/tags/list -u null:null", "{\"tags\": [\"2.0.0\"]}")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/postfix/tags/list -u null:null", "{\"tags\": [\"3.0.0\"]}")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/usermgt/tags/list -u null:null", "{\"tags\": [\"4.0.0\"]}")


        K3d sut = new K3d(scriptMock, "leWorkSpace", "leK3dWorkSpace", "path")

        // when
        def errorMsg = shouldFail(RuntimeException) {
            sut.setup(tag, [:], 1, 1)
        }

        // then
        assertThat(errorMsg.getMessage()).isEqualTo("failed to wait for deployment/k8s-blueprint-operator-controller-manager rollout: timeout")

        assertThat(scriptMock.actualEcho.get(10)).isEqualTo("configuring ecosystem core...")
        assertThat(scriptMock.actualEcho.get(11)).isEqualTo("Installing setup...")

        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("curl -H \"Metadata-Flavor: Google\" http://169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip")
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("whoami".trim())
    }

    @Test
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
        scriptMock.expectedShRetValueForScript.put("yq -oy -e '.Image' dogu.json | sed 's|registry\\.cloudogu\\.com\\(.\\+\\)|" + "myIP" + ".local:" + port + "\\1|g'", "myIP.local:5000/test/myimage:0.1.2")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get pod --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", "test-execpod")
        scriptMock.expectedShRetValueForScript.put("echo 'test-execpod' | grep 'test-execpod'", "test-execpod")
        scriptMock.expectedShRetValueForScript.put("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get deployment --template '{{range .items}}{{.metadata.name}}{{\"\\n\"}}{{end}}'", "test")
        scriptMock.expectedShRetValueForScript.put("echo 'test' | grep 'test'", "test")

        String doguYaml = """
apiVersion: k8s.cloudogu.com/v2
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
        assertThat(scriptMock.allActualArgs[9].trim()).isEqualTo("yq -oy -e '.Image' dogu.json | sed 's|registry\\.cloudogu\\.com\\(.\\+\\)|myIP.local:5000\\1|g'")
        assertThat(scriptMock.allActualArgs[10].trim()).isEqualTo("yq -oj '.Image=\"myIP.local:5000/test/myimage:0.1.2\"' dogu.json > leWorkspace/target/dogu.json")
        assertThat(scriptMock.allActualArgs[11].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl create configmap test-descriptor --from-file=leWorkspace/target/dogu.json")
        assertThat(scriptMock.allActualArgs[12].trim()).isEqualTo("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl apply -f \n" +
            "apiVersion: k8s.cloudogu.com/v2\n" +
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

    @Test
    void testK3d_collectAndArchiveLogs() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        def relevantResources = ["persistentvolumeclaim","statefulset","replicaset","deployment","service","secret","pod","configmap","persistentvolume","ingress","ingressclass"]
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
        assertThat(scriptMock.allActualArgs[i++].trim()).contains("rm -rf k3d_blueprint.yaml")
        assertThat(scriptMock.allActualArgs[i++].trim()).contains("rm -rf k3d_values.yaml")

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

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get configmap --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "configmap.yaml", "text": "value for configmap"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe configmap -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "configmap_description.yaml", "text": "value for configmap"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get persistentvolume --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "persistentvolume.yaml", "text": "value for persistentvolume"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe persistentvolume -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "persistentvolume_description.yaml", "text": "value for persistentvolume"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get ingress --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "ingress.yaml", "text": "value for ingress"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe ingress -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "ingress_description.yaml", "text": "value for ingress"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl get ingressclass --show-kind --ignore-not-found -l app=ces -o yaml || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "ingressclass.yaml", "text": "value for ingressclass"])

        assertThat(scriptMock.allActualArgs[i++].trim()).contains("sudo KUBECONFIG=leK3dWorkSpace/.k3d/.kube/config kubectl describe ingressclass -l app=ces || true")
        assertThat(scriptMock.writeFileParams[fileCounter++]).isEqualTo(["file": "ingressclass_description.yaml", "text": "value for ingressclass"])

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
        assertThat(scriptMock.archivedArtifacts.size()).isEqualTo(3)
        assertThat(scriptMock.archivedArtifacts[0]).isEqualTo(["artifacts":"k3d_blueprint.yaml"])
        assertThat(scriptMock.archivedArtifacts[1]).isEqualTo(["artifacts":"k3d_values.yaml"])
        assertThat(scriptMock.archivedArtifacts[2]).isEqualTo(["allowEmptyArchive":"true", "artifacts":"k8sLogs.zip"])

        assertThat(scriptMock.allActualArgs.size()).isEqualTo(i)
        assertThat(scriptMock.writeFileParams.size()).isEqualTo(25)
        assertThat(fileCounter).isEqualTo(25)
    }

    @Test
    void testK3d_applyDoguResource() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        def filename = "target/make/k8s/testName.yaml"
        def doguContentYaml = """
apiVersion: k8s.cloudogu.com/v2
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

    @Test
    void testK3d_configureSetupImage() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        scriptMock.expectedShRetValueForScript.put("whoami".toString(), "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        scriptMock.expectedShRetValueForScript.put("yq -i \".setup.image.registry = \\\"docker.io\\\"\" k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("yq -i \".setup.image.repository = \\\"foo/image\\\"\" k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("yq -i \".setup.image.tag = \\\"1.2.3\\\"\" k3d_values.yaml", "foo")

        // when
        sut.configureSetupImage("docker.io/foo/image:1.2.3")

        // then
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("whoami".trim())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("cat /etc/passwd | grep jenkins".trim())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("yq -i \".setup.image.registry = \\\"docker.io\\\"\" k3d_values.yaml".trim())
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("whoami".trim())
        assertThat(scriptMock.allActualArgs[4].trim()).isEqualTo("cat /etc/passwd | grep jenkins".trim())
        assertThat(scriptMock.allActualArgs[5].trim()).isEqualTo("yq -i \".setup.image.repository = \\\"foo/image\\\"\" k3d_values.yaml".trim())
        assertThat(scriptMock.allActualArgs[6].trim()).isEqualTo("whoami".trim())
        assertThat(scriptMock.allActualArgs[7].trim()).isEqualTo("cat /etc/passwd | grep jenkins".trim())
        assertThat(scriptMock.allActualArgs[8].trim()).isEqualTo("yq -i \".setup.image.tag = \\\"1.2.3\\\"\" k3d_values.yaml".trim())
    }

    @Test
    void testK3d_parseTags() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/ldap/tags/list -u null:null", "{\"tags\": [\"1.0.0\", \"1.0.1\"]}")
        scriptMock.expectedShRetValueForScript.put("curl https://registry.cloudogu.com/v2/official/cas/tags/list -u null:null", "{\"tags\": [\"2.0.0\", \"invalid\", \"2.0.1\"]}")

        List<String> deps = new ArrayList<>()
        deps.add("official/cas")
        deps.add("official/ldap:latest")
        deps.add("official/usermgt:3.0.0")

        // when
        String formatted = sut.formatDependencies(deps)

        // then
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("curl https://registry.cloudogu.com/v2/official/cas/tags/list -u null:null".trim())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("curl https://registry.cloudogu.com/v2/official/ldap/tags/list -u null:null".trim())
        assertThat(formatted.contains("cas\n        version: 2.0.1"))
        assertThat(formatted.contains("ldap\n        version: 1.0.1"))
        assertThat(formatted.contains("usermgt\n        version: 3.0.0"))

    }

    @Test
    void testK3d_configureComponentOperatorVersion() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        scriptMock.expectedShRetValueForScript.put("whoami".toString(), "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        scriptMock.expectedShRetValueForScript.put("yq -i \".component_operator_chart = \\\"test_ns/k8s-component-operator:1.2.3\\\"\" k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("yq -i \".component_operator_crd_chart = \\\"test_ns/k8s-component-operator-crd:4.5.6\\\"\" k3d_values.yaml", "foo")

        // when
        sut.configureComponentOperatorVersion('1.2.3', '4.5.6', 'test_ns')

        // then
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("whoami".trim())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("cat /etc/passwd | grep jenkins".trim())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("yq -i \".component_operator_chart = \\\"test_ns/k8s-component-operator:1.2.3\\\"\" k3d_values.yaml".trim())
        assertThat(scriptMock.allActualArgs[3].trim()).isEqualTo("whoami".trim())
        assertThat(scriptMock.allActualArgs[4].trim()).isEqualTo("cat /etc/passwd | grep jenkins".trim())
        assertThat(scriptMock.allActualArgs[5].trim()).isEqualTo("yq -i \".component_operator_crd_chart = \\\"test_ns/k8s-component-operator-crd:4.5.6\\\"\" k3d_values.yaml".trim())
    }

    @Test
    void testK3d_configureLogLevel() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        scriptMock.expectedShRetValueForScript.put("whoami".toString(), "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        scriptMock.expectedShRetValueForScript.put("yq -i \".logLevel = \\\"SUPER_ERROR\\\"\" k3d_values.yaml", "foo")

        // when
        sut.configureLogLevel("SUPER_ERROR")

        // then
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("whoami".trim())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("cat /etc/passwd | grep jenkins".trim())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("yq -i \".logLevel = \\\"SUPER_ERROR\\\"\" k3d_values.yaml".trim())
    }

    @Test
    void testK3d_configureComponents() {
        // given
        def workspaceDir = "leWorkspace"
        def k3dWorkspaceDir = "leK3dWorkSpace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, k3dWorkspaceDir, "path")

        scriptMock.expectedShRetValueForScript.put("whoami".toString(), "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        scriptMock.expectedShRetValueForScript.put("yq -i \".components.k8s-etcd.version = \\\"latest\\\"\" k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("yq -i \".components.k8s-etcd.helmRepositoryNamespace = \\\"k8s\\\"\" k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("yq -i \".components.k8s-promtail.version = \\\"1.2.3\\\"\" k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("yq -i \".components.k8s-promtail.helmRepositoryNamespace = \\\"test_ns\\\"\" k3d_values.yaml", "foo")
        scriptMock.expectedShRetValueForScript.put("yq -i \".components.k8s-blueprint-operator = null\" k3d_values.yaml", "foo")

        // when
        sut.configureComponents(["k8s-etcd"    : ["version": "latest", "helmRepositoryNamespace": "k8s"],
                                 "k8s-promtail": ["version": "1.2.3", "helmRepositoryNamespace": "test_ns"],
                                 "k8s-blueprint-operator": null])

        // then
        assertThat(scriptMock.allActualArgs[0].trim()).isEqualTo("whoami".trim())
        assertThat(scriptMock.allActualArgs[1].trim()).isEqualTo("cat /etc/passwd | grep jenkins".trim())
        assertThat(scriptMock.allActualArgs[2].trim()).isEqualTo("yq -i \".components.k8s-etcd.version = \\\"latest\\\" | .components.k8s-etcd.helmRepositoryNamespace = \\\"k8s\\\" | .components.k8s-promtail.version = \\\"1.2.3\\\" | .components.k8s-promtail.helmRepositoryNamespace = \\\"test_ns\\\" | .components.k8s-blueprint-operator = null\" k3d_values.yaml".trim())
    }
}
