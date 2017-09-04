package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue

class DockerTest {

    @Test
    void withRegistry() {

        Docker docker = create([ withRegistry: { String param1, String param2, Closure param3 ->
            return [param1, param2, param3]
        }])

        def args = docker.withRegistry("https://com.cloudogu/test", 'credentials') {
            return "fromClosure"
        }
        assertEquals("https://com.cloudogu/test", args[0])
        assertEquals('credentials', args[1])
        assertEquals("fromClosure", args[2]())
    }

    @Test
    void withRegistryWithoutCredentials() {
        Docker docker = create([ withRegistry: { String param1, String param2, Closure param3 ->
            return [param1, param2, param3]
        }])

        def args = docker.withRegistry("https://com.cloudogu/test") {
            return "fromClosure"
        }

        assertEquals("https://com.cloudogu/test", args[0])
        assertNull(args[1])
        assertEquals("fromClosure", args[2]())
    }

    @Test
    void withServer() {

        Docker docker = create([ withServer: { String param1, String param2, Closure param3 ->
            return [param1, param2, param3]
        }])

        def args = docker.withServer("https://com.cloudogu/test", 'credentials') {
            return "fromClosure"
        }
        assertEquals("https://com.cloudogu/test", args[0])
        assertEquals('credentials', args[1])
        assertEquals("fromClosure", args[2]())
    }

    @Test
    void withServerWithoutCredentials() {
        Docker docker = create([ withServer: { String param1, String param2, Closure param3 ->
            return [param1, param2, param3]
        }])

        def args = docker.withServer("https://com.cloudogu/test") {
            return "fromClosure"
        }

        assertEquals("https://com.cloudogu/test", args[0])
        assertNull(args[1])
        assertEquals("fromClosure", args[2]())
    }

    @Test
    void withTool() {
        Docker docker = create([ withTool: { String param1, Closure param2 ->
            return [param1, param2]
        }])

        def args = docker.withTool("m3") {
            return "fromClosure"
        }

        assertEquals("m3", args[0])
        assertEquals("fromClosure", args[1]())
    }

    @Test
    void image() {
        Docker docker = create([ image: { String param1 ->
            return param1
        }])

        def ret = docker.image('google/cloud-sdk:164.0.0')

        assertEquals('google/cloud-sdk:164.0.0', ret)
    }

    @Test
    void build() {
        Docker docker = create([ build: { String param1, String param2 ->
            return [param1, param2]
        }])

        def args = docker.build("com.cloudogu/test/app:123", "app")

        assertEquals("com.cloudogu/test/app:123", args[0])
        assertEquals("app", args[1])
    }

    @Test
    void buildWithoutArgs() {
        Docker docker = create([ build: { String param1, String param2 ->
            return [param1, param2]
        }])

        def args = docker.build("com.cloudogu/test/app:123")

        assertEquals("com.cloudogu/test/app:123", args[0])
        assertEquals(".", args[1])
    }

    @Test
    void findIp() {
        String containerId = '93a401b14684'
        Docker docker = new Docker( [ sh: { Map<String, String> args -> return args['script'] } ])
        def actualIp = docker.findIp([id: containerId])
        assertTrue(actualIp.contains(containerId))
    }

    private Docker create(Map<String, Closure> mockedMethod) {
        Map<String, Map<String, Closure>> mockedScript = [
                docker: mockedMethod
        ]
        return new Docker(mockedScript)
    }
}
