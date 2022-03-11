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

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("sudo KUBECONFIG=leWorkspace/k3d/.k3d/.kube/config kubectl get nodes")
    }

    void testSetupK3d() {
        def workspace="leWorkspace"
        def k3dVer="1.2.3"

        def scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("sed -n 's/^K3D_VERSION=//p' ${workspace}/k3d/scripts/init-cluster.sh".toString(), "${k3dVer}".toString())

        K3d sut = new K3d(scriptMock,"${workspace}", "path")

        sut.setupK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("rm -rf ${workspace}/k3d".toString())
        assertThat(scriptMock.actualShStringArgs[1].trim()).isEqualTo("mkdir -p ${workspace}/k3d/.k3d/bin".toString())
        assertThat(scriptMock.actualShStringArgs[2].trim()).isEqualTo("curl -s https://raw.githubusercontent.com/rancher/k3d/main/install.sh | TAG=v${k3dVer} K3D_INSTALL_DIR=${workspace}/k3d/.k3d/bin bash -s -- --no-sudo".toString())
        assertThat(scriptMock.actualShStringArgs[3].trim()).startsWith("yes | ${workspace}/k3d/scripts/init-cluster.sh --cluster-name=citest-".toString())
    }
}
