package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static org.junit.Assert.assertEquals

class DockerTest {

    @Test
    void testMethodMissing() {
        Map<String, Map<String, Closure>> mockedScript = [
                docker: [
                        someMethod: { String param1, int param2 ->
                            return [param1, param2]
                        },
                ]
        ]

        Docker docker = new Docker(mockedScript)
        def args = docker.someMethod("param1", 2)
        assertEquals("param1", args[0])
        assertEquals(2, args[1])
    }
}
