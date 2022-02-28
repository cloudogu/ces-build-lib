package com.cloudogu.ces.cesbuildlib

class k3dTest extends GroovyTestCase {
    void testCreateClusterName() {
        K3d sut = new K3d("workspace", "clusterName")
        String testClusterName = sut.createClusterName()
        assertTrue(testClusterName.contains("citest-"))
    }
}
