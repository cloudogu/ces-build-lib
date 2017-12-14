package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.*

class DockerTest {

    def expectedImage = 'google/cloud-sdk:164.0.0'
    def expectedHome = '/home/jenkins'
    def actualPasswd = 'jenkins:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash'
    Map<String, String> actualWriteFileArgs = [:]

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
        def env = docker.findEnv([id: containerId])
        assertTrue(env.contains(containerId))
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
        Docker docker = createWithImage(
                [inside: { String param1, Closure param2 ->
                    return [param1, param2]
                }])

        def args = docker.image(expectedImage).inside('-v a:b') { return 'expectedClosure' }

        assertEquals('-v a:b', args[0])
        assertEquals('expectedClosure', args[1].call())
    }

    @Test
    void imageId() {
        Docker docker = createWithImage([imageName: { expectedImage }])

        def args = docker.image(expectedImage).id

        assertEquals(expectedImage, args)
    }

    @Test
    void imageName() {
        Docker docker = createWithImage([imageName: { expectedImage }])

        def args = docker.image(expectedImage).imageName()

        assertEquals(expectedImage, args)
    }

    @Test
    void imageRun() {
        Docker docker = createWithImage(
                [run: { String param1, String param2 ->
                    return [param1, param2]
                }])

        def args = docker.image(expectedImage).run('arg', 'cmd')

        assertEquals('arg', args[0])
        assertEquals('cmd', args[1])
    }

    @Test
    void imageWithRun() {
        Docker docker = createWithImage(
                [withRun: { String param1, String param2, Closure param3 ->
                    return [param1, param2, param3]
                }])

        def args = docker.image(expectedImage).withRun('arg', 'cmd')  { return 'expectedClosure' }

        assertEquals('arg', args[0])
        assertEquals('cmd', args[1])
        assertEquals('expectedClosure', args[2].call())
    }

    @Test
    void imageInsideMountJenkinsUser() {
        Docker docker = createWithImage(
                [inside: { String param1, Closure param2 ->
                    return [param1, param2]
                }])

        def image = docker.image(expectedImage)
        image.mountJenkinsUser = true
        def args = image.inside('-v a:b') { return 'expectedClosure' }

        assertEquals('-v a:b -v /home/jenkins/.jenkins/passwd:/etc/passwd:ro', args[0])
        assertEquals('expectedClosure', args[1].call())
        assertEquals('jenkins:x:1000:1000::/home/jenkins:/bin/sh', actualWriteFileArgs['text'])
    }

    @Test
    void imageRunMountJenkinsUser() {
        Docker docker = createWithImage(
                [run: { String param1, String param2 ->
                    return [param1, param2]
                }])

        def image = docker.image(expectedImage)
        image.mountJenkinsUser = true
        def args = image.run('arg', 'cmd')

        assertEquals('arg -v /home/jenkins/.jenkins/passwd:/etc/passwd:ro', args[0])
        assertEquals('cmd', args[1])
        assertEquals('jenkins:x:1000:1000::/home/jenkins:/bin/sh', actualWriteFileArgs['text'])
    }

    @Test
    void imageWithRunMountJenkinsUser() {
        Docker docker = createWithImage(
                [withRun: { String param1, String param2, Closure param3 ->
                    return [param1, param2, param3]
                }])

        def image = docker.image(expectedImage)
        image.mountJenkinsUser = true
        def args = image.withRun('arg', 'cmd')  { return 'expectedClosure' }

        assertEquals('arg -v /home/jenkins/.jenkins/passwd:/etc/passwd:ro', args[0])
        assertEquals('cmd', args[1])
        assertEquals('expectedClosure', args[2].call())
        assertEquals('jenkins:x:1000:1000::/home/jenkins:/bin/sh', actualWriteFileArgs['text'])
    }

    @Test
    void imageMountJenkinsUserUnexpectedPasswd() {
        testForInvaildPasswd('jenkins:x:1000:1000',
                '/etc/passwd entry for current user does not match user:x:uid:gid:')
    }

    @Test
    void imageMountJenkinsUserPasswdEmpty() {
        testForInvaildPasswd('',
                'Unable to parse user jenkins from /etc/passwd.')
    }

    private Docker create(Map<String, Closure> mockedMethod) {
        Map<String, Map<String, Closure>> mockedScript = [
                docker: mockedMethod
        ]
        return new Docker(mockedScript)
    }

    private Docker createWithImage(Map<String, Closure> mockedMethod) {

        def mockedScript = [
                docker: [image: { String id ->
                    assert id == expectedImage
                    mockedMethod.put('id', id)
                    return mockedMethod
                }
                ]
        ]
        mockedScript.put('sh', { Map<String, String> args ->
            assert args['script'].contains(System.properties.'user.name')
            return actualPasswd })
        mockedScript.put('pwd', { return expectedHome })
        mockedScript.put('writeFile', { Map<String, String> args -> actualWriteFileArgs = args})
        mockedScript.put('error', { String arg -> throw new RuntimeException(arg) })

        return new Docker(mockedScript)
    }

    private void testForInvaildPasswd(String invalidPasswd, String expectedError) {
        Docker docker = createWithImage(
                [run: { String param1, String param2 ->
                    return [param1, param2]
                }])

        actualPasswd = invalidPasswd

        def image = docker.image(expectedImage)
        image.mountJenkinsUser = true
        def exception = shouldFail {
            image.run('arg', 'cmd')
        }

        assertEquals(expectedError, exception.getMessage())
    }
}
