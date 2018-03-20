package com.cloudogu.ces.cesbuildlib

import org.junit.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.*

class DockerTest {

    def expectedImage = 'google/cloud-sdk:164.0.0'
    def expectedHome = '/home/jenkins'
    def actualUser = 'jenkins'
    def actualPasswd = "$actualUser:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash"
    def actualDockerGroupId = "999"
    def actualDockerGroup = "docker:x:$actualDockerGroupId:jenkins"
    Map<String, String> actualWriteFileArgs = [:]
    def actualShArgs = new LinkedList<Object>()

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

        def args = docker.image(expectedImage).withRun('arg', 'cmd') { return 'expectedClosure' }

        assertEquals('arg', args[0])
        assertEquals('cmd', args[1])
        assertEquals('expectedClosure', args[2].call())
    }

    @Test
    void imageInsideExtendedArgs() {
        def args = testExtendedArgs {
            Docker.Image image -> return image.inside('-v a:b') { return 'expectedClosure' }
        }

        // inside() params
        assert args[0].contains('-v a:b ')
        assert 'expectedClosure' == args[1].call()
    }

    private testExtendedArgs(Closure<Docker.Image> testImage) {
        Docker docker = createWithImage(
                [inside: { String param1, Closure param2 ->
                    return [param1, param2]
                },
                 run: { String param1, String param2 ->
                     return [param1, param2]
                 }
                ])

        def image = docker.image(expectedImage)
                .mountJenkinsUser()
                .mountDockerSocket()
                .installDockerClient('1.2.3')

        def args = testImage.call(image)

        // extended arg mounts
        assert args[0].contains('-v /home/jenkins/.jenkins/etc/passwd:/etc/passwd:ro ')
        assert args[0].contains('-v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=\"unix:///var/run/docker.sock\" -v /home/jenkins/.jenkins/etc/group:/etc/group:ro --group-add 999 ')
        assert args[0].contains("-v $expectedHome/.jenkins/docker/docker:/usr/bin/docker")

        // Docker installed
        assert actualShArgs.size() > 0
        assert actualShArgs.get(0).contains('https://download.docker.com/linux/static/stable/x86_64/docker-1.2.3-ce.tgz')

        // Written files
        assert 'jenkins:x:1000:1000::/home/jenkins:/bin/sh' == actualWriteFileArgs['.jenkins/etc/passwd']
        assert actualDockerGroup == actualWriteFileArgs['.jenkins/etc/group']

        return args
    }

    @Test
    void imageRunExtendedArgs() {
        def args = testExtendedArgs {
            Docker.Image image -> return image.run('arg', 'cmd')
        }

        // run() params
        assert args[0].contains('arg ')
        assert 'cmd' == args[1]
    }

    @Test
    void imageWithRunExtendedArgs() {
        Docker docker = createWithImage(
                [withRun: { String param1, String param2, Closure param3 ->
                    return [param1, param2, param3]
                }])

        def args = docker.image(expectedImage)
                .mountJenkinsUser()
                .mountDockerSocket()
                .withRun('arg', 'cmd') { return 'expectedClosure' }

        // withRun() params
        assert 'cmd' == args[1]
        assert 'expectedClosure' == args[2].call()

        // extended arg mounts
        assert args[0].contains('-v /home/jenkins/.jenkins/etc/passwd:/etc/passwd:ro ')
        assert args[0].contains('-v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=\"unix:///var/run/docker.sock\" -v /home/jenkins/.jenkins/etc/group:/etc/group:ro --group-add 999 ')

        // Written files
        assert 'jenkins:x:1000:1000::/home/jenkins:/bin/sh' == actualWriteFileArgs['.jenkins/etc/passwd']
        assert actualDockerGroup == actualWriteFileArgs['.jenkins/etc/group']
    }

    @Test
    void imageMountJenkinsUserUnexpectedPasswd() {
        actualPasswd = 'jenkins:x:1000:1000'
        testForInvaildPasswd(
                { image -> image.mountJenkinsUser() },
                '/etc/passwd entry for current user does not match user:x:uid:gid:')
    }

    @Test
    void imageMountJenkinsUserPasswdEmpty() {
        actualPasswd = ''
        testForInvaildPasswd(
                { image -> image.mountJenkinsUser() },
                'Unable to parse user jenkins from /etc/passwd.')
    }

    @Test
    void imageMountDockerSocketPasswdEmpty() {
        actualDockerGroup = ''
        testForInvaildPasswd(
                { image -> image.mountDockerSocket() },
                'Unable to parse group docker from /etc/group. Docker host will not be accessible for container.')
    }

    private Docker create(Map<String, Closure> mockedMethod) {
        Map<String, Map<String, Closure>> mockedScript = [
                docker: mockedMethod
        ]
        return new Docker(mockedScript)
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private Docker createWithImage(Map<String, Closure> mockedMethod) {

        def mockedScript = [
                docker: [image: { String id ->
                    assert id == expectedImage
                    mockedMethod.put('id', id)
                    return mockedMethod
                }
                ]
        ]
        mockedScript.put('sh', { Object args ->

            if (!(args instanceof Map)) {
                actualShArgs.add(args)
                return
            }

            String script = args['script']
            if (script.contains('cat /etc/passwd ')) {
                assert script.contains(actualUser)
            }
            if (script == 'whoami') return actualUser
            if (script == 'cat /etc/group | grep docker') return actualDockerGroup
            if (script.contains(actualDockerGroup)) return actualDockerGroupId
            if (script.contains('cat /etc/passwd | grep')) return actualPasswd
            else fail("Unexpected sh call. Script: " + script)
        })
        mockedScript.put('pwd', { return expectedHome })
        mockedScript.put('writeFile', { Map<String, String> args -> actualWriteFileArgs.put(args['file'], args['text']) })
        mockedScript.put('error', { String arg -> throw new RuntimeException(arg) })

        return new Docker(mockedScript)
    }

    private void testForInvaildPasswd(Closure imageHook, String expectedError) {
        Docker docker = createWithImage(
                [run: { String param1, String param2 ->
                    return [param1, param2]
                }])

        def exception = shouldFail {
            def image = docker.image(expectedImage)
            imageHook.call(image)
            image.run('arg', 'cmd')
        }

        assertEquals(expectedError, exception.getMessage())
    }
}
