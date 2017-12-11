package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static org.junit.Assert.*

class DockerTest {

    @Test
    void findIp() {
        String containerId = '93a401b14684'
        Docker docker = new Docker([sh: { Map<String, String> args -> return args['script'] }])
        def ip = docker.findIp([id: containerId])
        assertTrue(ip.contains(containerId))
    }

    @Test
    void findEnv() {
        String containerId = '93a401b14684'
        Docker docker = new Docker([sh: { Map<String, String> args -> return args['script'] }])
        def evn = docker.findEnv([id: containerId])
        assertTrue(evn.contains(containerId))
    }

    @Test
    void isRunning() {
        String containerId = '93a401b14684'
        Docker docker = new Docker([sh: { Map<String, String> args -> return "true" }])
        assertTrue(docker.isRunning([id: containerId]))
    }

    @Test
    void withRegistry() {

        Docker docker = create([withRegistry: { String param1, String param2, Closure param3 ->
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
        Docker docker = create([withRegistry: { String param1, String param2, Closure param3 ->
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

        Docker docker = create([withServer: { String param1, String param2, Closure param3 ->
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
        Docker docker = create([withServer: { String param1, String param2, Closure param3 ->
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
        Docker docker = create([withTool: { String param1, Closure param2 ->
            return [param1, param2]
        }])

        def args = docker.withTool("m3") {
            return "fromClosure"
        }

        assertEquals("m3", args[0])
        assertEquals("fromClosure", args[1]())
    }

    @Test
    void build() {
        Docker docker = create([build: { String param1, String param2 ->
            return [param1, param2]
        }])

        def args = docker.build("com.cloudogu/test/app:123", "app")

        assertEquals("com.cloudogu/test/app:123", args[0])
        assertEquals("app", args[1])
    }

    @Test
    void buildWithoutArgs() {
        Docker docker = create([build: { String param1, String param2 ->
            return [param1, param2]
        }])

        def args = docker.build("com.cloudogu/test/app:123")

        assertEquals("com.cloudogu/test/app:123", args[0])
        assertEquals(".", args[1])
    }

    @Test
    void imageInside() {
        def expectedImage = 'google/cloud-sdk:164.0.0'

        Docker docker = createWithImage(expectedImage,
                [inside: { String param1, Closure param2 ->
                    return [param1, param2]
                }])

        def args = docker.image(expectedImage).inside('-v a:b') { return 'expectedClosure' }

        assertEquals('-v a:b', args[0])
        assertEquals('expectedClosure', args[1].call())
    }

    @Test
    void imageId() {
        def expectedImage = 'google/cloud-sdk:164.0.0'

        Docker docker = createWithImage(expectedImage, [imageName: { expectedImage }])

        def args = docker.image(expectedImage).id

        assertEquals(expectedImage, args)
    }

    @Test
    void imageName() {
        def expectedImage = 'google/cloud-sdk:164.0.0'

        Docker docker = createWithImage(expectedImage, [imageName: { expectedImage }])

        def args = docker.image(expectedImage).imageName()

        assertEquals(expectedImage, args)
    }

    @Test
    void imageRun() {
        def expectedImage = 'google/cloud-sdk:164.0.0'

        Docker docker = createWithImage(expectedImage,
                [run: { String param1, String param2 ->
                    return [param1, param2]
                }])

        def args = docker.image(expectedImage).run('arg', 'cmd')

        assertEquals('arg', args[0])
        assertEquals('cmd', args[1])
    }

    @Test
    void imageWithRun() {
        def expectedImage = 'google/cloud-sdk:164.0.0'

        Docker docker = createWithImage(expectedImage,
                [withRun: { String param1, String param2, Closure param3 ->
                    return [param1, param2, param3]
                }])

        def args = docker.image(expectedImage).withRun('arg', 'cmd')  { return 'expectedClosure' }

        assertEquals('arg', args[0])
        assertEquals('cmd', args[1])
        assertEquals('expectedClosure', args[2].call())
    }

    private Docker create(Map<String, Closure> mockedMethod) {
        Map<String, Map<String, Closure>> mockedScript = [
                docker: mockedMethod
        ]
        return new Docker(mockedScript)
    }

    private Docker createWithImage(String expectedImage, Map<String, Closure> mockedMethod) {

        def mockedScript = [
                docker: [image: { String id ->
                    assert id == expectedImage
                    mockedMethod.put('id', id)
                    return mockedMethod
                }
                ]
        ]
        return new Docker(mockedScript)
    }
}
