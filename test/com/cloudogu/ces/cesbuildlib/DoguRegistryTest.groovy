package com.cloudogu.ces.cesbuildlib

class DoguRegistryTest extends GroovyTestCase {

    void testCreateRegistryObjectWithDefaults() {
        // given
        // when
        DoguRegistry sut = new DoguRegistry("script")

        // then
        assertTrue(sut != null)
    }
}
