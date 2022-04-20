package com.cloudogu.ces.cesbuildlib

import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static org.assertj.core.api.Assertions.*
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class K3dTest extends GroovyTestCase {
    void testCreateClusterName() {
        K3d sut = new K3d("script", "workspace", "path")
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

        K3d sut = new K3d(scriptMock, "workspace", "path")

        // we need to create a registry so that the deletion of the registry is triggered
        sut.startK3d()

        // when
        sut.deleteK3d()

        // then
        assertThat(scriptMock.actualShStringArgs[11].trim()).contains("k3d registry delete citest-")
        assertThat(scriptMock.actualShStringArgs[12].trim()).contains("k3d cluster delete citest-")
        assertThat(scriptMock.actualShStringArgs.size()).isEqualTo(13)
    }

    void testKubectl() {
        String workspaceDir = "leWorkspace"
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock, workspaceDir, "path")

        sut.kubectl("get nodes")

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl get nodes".trim())
        assertThat(scriptMock.actualShStringArgs.size()).isEqualTo(1)
    }

    void testStartK3d() {
        def workspaceDir = "leWorkspace"
        def k3dVer = "4.4.7"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, "${workspaceDir}", "path")

        sut.startK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("rm -rf ${workspaceDir}/.k3d".toString())
        assertThat(scriptMock.actualShStringArgs[1].trim()).isEqualTo("mkdir -p ${workspaceDir}/.k3d/bin".toString())
        assertThat(scriptMock.actualShStringArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${k3dVer} K3D_INSTALL_DIR=${workspaceDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.actualShStringArgs[3].trim()).matches("k3d registry create citest-[0-9a-f]+ --port 54321")
        assertThat(scriptMock.actualShStringArgs[4].trim()).startsWith("k3d cluster create citest-")
        assertThat(scriptMock.actualShStringArgs[5].trim()).startsWith("k3d kubeconfig merge citest-")
        assertThat(scriptMock.actualShStringArgs[6].trim()).startsWith("echo \"Using credentials: cesmarvin-setup\"")
        assertThat(scriptMock.actualShStringArgs[7].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-dogu-registry || true")
        assertThat(scriptMock.actualShStringArgs[8].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-docker-registry || true")
        assertThat(scriptMock.actualShStringArgs[9].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"null\" --from-literal=password=\"null\"")
        assertThat(scriptMock.actualShStringArgs[10].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl create secret docker-registry k8s-dogu-operator-docker-registry --docker-server=\"registry.cloudogu.com\" --docker-benutzername=\"null\" --docker-email=\"a@b.c\" --docker-password=\"null\"")
        assertThat(scriptMock.actualShStringArgs.size()).isEqualTo(11)
    }

    void testStartK3dWithCustomCredentials() {
        def workspaceDir = "leWorkspace"
        def k3dVer = "4.4.7"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock, "${workspaceDir}", "path", "myBackendCredentialsID")

        sut.startK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("rm -rf ${workspaceDir}/.k3d".toString())
        assertThat(scriptMock.actualShStringArgs[1].trim()).isEqualTo("mkdir -p ${workspaceDir}/.k3d/bin".toString())
        assertThat(scriptMock.actualShStringArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${k3dVer} K3D_INSTALL_DIR=${workspaceDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.actualShStringArgs[3].trim()).matches("k3d registry create citest-[0-9a-f]+ --port 54321")
        assertThat(scriptMock.actualShStringArgs[4].trim()).startsWith("k3d cluster create citest-")
        assertThat(scriptMock.actualShStringArgs[5].trim()).startsWith("k3d kubeconfig merge citest-")
        assertThat(scriptMock.actualShStringArgs[6].trim()).startsWith("echo \"Using credentials: myBackendCredentialsID\"")
        assertThat(scriptMock.actualShStringArgs[7].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-dogu-registry || true")
        assertThat(scriptMock.actualShStringArgs[8].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl delete secret k8s-dogu-operator-docker-registry || true")
        assertThat(scriptMock.actualShStringArgs[9].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl create secret generic k8s-dogu-operator-dogu-registry --from-literal=endpoint=\"https://dogu.cloudogu.com/api/v2/dogus\" --from-literal=username=\"null\" --from-literal=password=\"null\"")
        assertThat(scriptMock.actualShStringArgs[10].trim()).startsWith("sudo KUBECONFIG=${workspaceDir}/.k3d/.kube/config kubectl create secret docker-registry k8s-dogu-operator-docker-registry --docker-server=\"registry.cloudogu.com\" --docker-benutzername=\"null\" --docker-email=\"a@b.c\" --docker-password=\"null\"")
        assertThat(scriptMock.actualShStringArgs.size()).isEqualTo(11)
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
                Closure doThings = (Closure)invocation.getArgument(1)
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

        K3d sut = new K3d(scriptMock, "leWorkspace", "path")
        sut.startK3d()

        // when
        sut.buildAndPushToLocalRegistry(imageName, imageTag)

        // then
        assertThat(scriptMock.actualShStringArgs[11].trim()).isEqualTo("image pushed".toString())
        assertThat(scriptMock.actualShStringArgs.size()).isEqualTo(12)
    }
}
