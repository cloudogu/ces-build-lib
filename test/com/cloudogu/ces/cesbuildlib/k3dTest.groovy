package com.cloudogu.ces.cesbuildlib

class k3dTest extends GroovyTestCase {
    void testCreateClusterName() {
        K3d sut = new K3d("script","workspace", "path")
        String testClusterName = sut.createClusterName()
        assertTrue(testClusterName.contains("citest-"))
        assertTrue(testClusterName != "citest-")
        assertTrue(testClusterName.length() <= 32)
        String testClusterName2 = sut.createClusterName()
        assertTrue(testClusterName != testClusterName2)
    }
}
