package com.cloudogu.ces.cesbuildlib
import static org.assertj.core.api.Assertions.assertThat

class K3dTest extends GroovyTestCase {
    void testCreateClusterName() {
        K3d sut = new K3d("script","workspace", "path", "credentials")
        String testClusterName = sut.createClusterName()
        assertTrue(testClusterName.contains("citest-"))
        assertTrue(testClusterName != "citest-")
        assertTrue(testClusterName.length() <= 32)
        String testClusterName2 = sut.createClusterName()
        assertTrue(testClusterName != testClusterName2)
    }

    void testDeleteK3d() {
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock,"workspace", "path", "credentials")

        sut.deleteK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).contains("k3d cluster delete citest-")
    }

    void testKubectl() {
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock,"leWorkspace", "path", "credentials")

        sut.kubectl("get nodes")

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("sudo KUBECONFIG=leWorkspace/.k3d/.kube/config kubectl get nodes")
    }

    void testSetupK3d() {
        def gitOpsPlaygroundDir="leWorkspace"
        def k3dVer="1.2.3"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("sed -n 's/^K3D_VERSION=//p' ${gitOpsPlaygroundDir}/scripts/init-cluster.sh".toString(), "${k3dVer}".toString())

        K3d sut = new K3d(scriptMock,"${gitOpsPlaygroundDir}", "path", "credentials")

        sut.setupK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("rm -rf ${gitOpsPlaygroundDir}".toString())
        assertThat(scriptMock.actualShStringArgs[1].trim()).isEqualTo("mkdir -p ${gitOpsPlaygroundDir}/.k3d/bin".toString())
        assertThat(scriptMock.actualShStringArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${k3dVer} K3D_INSTALL_DIR=${gitOpsPlaygroundDir}/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.actualShStringArgs[3].trim()).startsWith("yes | ${gitOpsPlaygroundDir}/scripts/init-cluster.sh --cluster-name=citest-".toString())
    }
}
