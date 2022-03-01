package com.cloudogu.ces.cesbuildlib

class k3dTest extends GroovyTestCase {
    void testCreateClusterName() {
        K3d sut = new K3d("script","workspace", "path")
        String testClusterName = sut.createClusterName()
        assertTrue(testClusterName.contains("citest-"))
    }
}
