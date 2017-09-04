package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static groovy.util.GroovyTestCase.assertEquals

class ShTest {

    @Test
    void testReturnStdOut() throws Exception {
        Sh sh = new Sh( [ sh: { Map<String, String> args -> return args['script'] } ])

        def result = sh.returnStdOut 'echo abc \n '
        assertEquals('echo abc', result)
    }
}