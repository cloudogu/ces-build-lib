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

    void testInstallKubectl() {
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock,"workspace", "path", "credentials")

        sut.installKubectl()

        assertThat(scriptMock.actualShStringArgs[0].trim()).isEqualTo("sudo snap install kubectl --classic")
    }

    void testDeleteK3d() {
        def scriptMock = new ScriptMock()
        K3d sut = new K3d(scriptMock,"workspace", "path", "credentials")

        sut.deleteK3d()

        assertThat(scriptMock.actualShStringArgs[0].trim()).contains("k3d cluster delete citest-")
    }
}
