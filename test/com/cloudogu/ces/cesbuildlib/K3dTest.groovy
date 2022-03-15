package com.cloudogu.ces.cesbuildlib
import static org.assertj.core.api.Assertions.assertThat

class K3dTest extends GroovyTestCase {
    void testCreateClusterName() {
        K3d sut = new K3d("script","workspace", "path")
        String testClusterName = sut.createClusterName()
        assertTrue(testClusterName.contains("citest-"))
        assertTrue(testClusterName != "citest-")
        assertTrue(testClusterName.length() <= 32)
        String testClusterName2 = sut.createClusterName()
        assertTrue(testClusterName != testClusterName2)
    }

    void testInstallKubectl() {
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock,"workspace", "path")

        sut.installKubectl()

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("sudo snap install kubectl --classic")
    }

    void testDeleteK3d() {
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock,"workspace", "path")

        sut.deleteK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).contains("k3d cluster delete citest-")
    }

    void testKubectl() {
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock,"leWorkspace", "path")

        sut.kubectl("get nodes")

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("sudo KUBECONFIG=leWorkspace/.k3d/.kube/config kubectl get nodes")
    }

    void testStartK3d() {
        def workspaceDir="leWorkspace"
        def k3dVer="4.4.7"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put('echo -n $(python3 -c \'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()\');'.toString(), "54321")

        K3d sut = new K3d(scriptMock,"${workspaceDir}", "path")

        sut.startK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("rm -rf ${workspaceDir}/.k3d".toString())
        assertThat(scriptMock.actualShStringArgs[1].trim()).isEqualTo("mkdir -p ${workspaceDir}/.k3d/bin".toString())
        assertThat(scriptMock.actualShStringArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${k3dVer} K3D_INSTALL_DIR=${workspaceDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.actualShStringArgs[3].trim()).matches("k3d registry create citest-[0-9a-f]+ --port 54321")
        assertThat(scriptMock.actualShStringArgs[4].trim()).startsWith("k3d cluster create citest-")
        assertThat(scriptMock.actualShStringArgs[5].trim()).startsWith("k3d kubeconfig merge citest-")
    }
}
