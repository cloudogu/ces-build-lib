package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static groovy.util.GroovyTestCase.assertEquals

class ShTest {

    @Test
    void testCall() throws Exception {
        Sh sh = new Sh( [ sh: { Map<String, String> args -> return args['script'] } ])

        def result = sh 'echo abc \n '
        assertEquals('echo abc', result)
    }
}