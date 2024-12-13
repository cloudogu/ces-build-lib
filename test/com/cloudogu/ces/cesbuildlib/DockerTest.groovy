package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.Test

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.jupiter.api.Assertions.*

class DockerTest {

    def expectedImage = 'google/cloud-sdk:164.0.0'
    def expectedHome = '/home/jenkins'
    def actualUser = 'jenkins'
    def actualDockerServerVersion = '19.03.8'
    def actualPasswd = "$actualUser:x:1000:1000:Jenkins,,,:/home/jenkins:/bin/bash"
    def actualDockerGroupId = "999"
    def actualDockerGroup = "docker:x:$actualDockerGroupId:jenkins"
    Map<String, String> actualWriteFileArgs = [:]
    def actualShArgs = new LinkedList<Object>()
    def actualRepoDigests = ''
    def actualPushParams
    def actualTagParams

    @Test
    void findIpOfContainer() {
        String containerId = '93a401b14684'
        Docker docker = new Docker([sh: { Map<String, String> args -> return args['script'] }])
        def ip = docker.findIp([id: containerId])
        assertTrue(ip.contains(containerId))
    }

    @Test
    void findIpInContainer() {
        String expectedIp = '172.2.0.2'
        Docker docker = new Docker([sh: { Map<String, String> args ->
            if (args['script'] == 'cat /proc/1/cgroup | grep docker >/dev/null 2>&1') 0
            else if (args['script'] == 'hostname -I | awk \'{print $1}\'') expectedIp
        }])
        def ip = docker.findIp()
        assert ip == expectedIp
    }

    @Test
    void findIpOnHost() {
        String expectedIp = '172.2.0.1'
        Docker docker = new Docker([sh: { Map<String, String> args ->
            if (args['script'] == 'cat /proc/1/cgroup | grep docker >/dev/null 2>&1') 1
            else if (args['script'] == 'ip route show | awk \'/docker0/ {print $9}\'') expectedIp
        }])
        def ip = docker.findIp()
        assert ip == expectedIp
    }

    @Test
    void findDockerHostIpInContainer() {
        String expectedIp = '172.2.0.1'
        Docker docker = new Docker([sh: { Map<String, String> args ->
            if (args['script'] == 'cat /proc/1/cgroup | grep docker >/dev/null 2>&1') 0
            else if (args['script'] == 'ip route show | awk \'/default/ {print $3}\'') expectedIp
        }])
        def ip = docker.findDockerHostIp()
        assert ip == expectedIp
    }

    @Test
    void findDockerHostIpOnHost() {
        String expectedIp = '172.2.0.1'
        Docker docker = new Docker([sh: { Map<String, String> args ->
            if (args['script'] == 'cat /proc/1/cgroup | grep docker >/dev/null 2>&1') 1
            else if (args['script'] == 'ip route show | awk \'/docker0/ {print $9}\'') expectedIp
        }])
        def ip = docker.findDockerHostIp()
        assert ip == expectedIp
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
        Docker docker = createWithImage(mockedImageMethodInside())

        def args = docker.image(expectedImage).inside { return 'expectedClosure' }

        assertEquals(' --entrypoint="" ', args[0])
        assertEquals('expectedClosure', args[1].call())
    }

    @Test
    void imageInsideWithArgs() {
        Docker docker = createWithImage(mockedImageMethodInside())

        def args = docker.image(expectedImage).inside('-v a:b') { return 'expectedClosure' }

        assert args[0].startsWith('-v a:b')
        assertEquals('expectedClosure', args[1].call())
    }

   @Test
    void imageInsideWithAdditionalRunArgs() {

        Docker docker = createWithImage(mockedImageMethodInside())

        docker.script += [
            env:  [
                ADDITIONAL_DOCKER_RUN_ARGS: '-u 0:0'
            ]
        ]

        def args = docker.image(expectedImage).inside('') { return 'expectedClosure' }

        assert args[0].contains('-u 0:0')
        assertEquals('expectedClosure', args[1].call())
    }


    @Test
    void imageInsideWithEntrypoint() {
        Docker docker = createWithImage(mockedImageMethodInside())

        def args = docker.image(expectedImage).inside('--entrypoint="entry"') { return 'expectedClosure' }

        assertEquals('--entrypoint="entry"', args[0])
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
        Docker docker = createWithImage(mockedImageMethodRun())

        def args = docker.image(expectedImage).run('arg', 'cmd')

        assertEquals('arg --entrypoint="" ', args[0])
        assertEquals('cmd', args[1])
    }

    @Test
    void imageWithRun() {
        Docker docker = createWithImage(mockedImageMethodWithRun())

        def args = docker.image(expectedImage).withRun('arg', 'cmd') { return 'expectedClosure' }

        assertEquals('arg --entrypoint="" ', args[0])
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
        Docker docker = createWithImage(mockedImageMethodInside() + mockedImageMethodRun())

        def image = docker.image(expectedImage)
                .mountJenkinsUser()
                .mountDockerSocket()
                .installDockerClient('18.03.1')

        def args = testImage.call(image)

        // extended arg mounts
        assert args[0].contains('-v /home/jenkins/.jenkins/etc/passwd:/etc/passwd:ro ')
        assert args[0].contains('-v /var/run/docker.sock:/var/run/docker.sock -v /home/jenkins/.jenkins/etc/group:/etc/group:ro --group-add 999 ')
        assert args[0].contains("-v $expectedHome/.jenkins/docker/docker:/usr/bin/docker")

        // Docker installed
        assert actualShArgs.size() > 0
        assert actualShArgs.get(0).contains('https://download.docker.com/linux/static/stable/x86_64/docker-18.03.1-ce.tgz')

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
        Docker docker = createWithImage(mockedImageMethodWithRun())

        def args = docker.image(expectedImage)
                .mountJenkinsUser()
                .mountDockerSocket()
                .withRun('arg', 'cmd') { return 'expectedClosure' }

        // withRun() params
        assert 'cmd' == args[1]
        assert 'expectedClosure' == args[2].call()

        // extended arg mounts
        assert args[0].contains('-v /home/jenkins/.jenkins/etc/passwd:/etc/passwd:ro ')
        assert args[0].contains('-v /var/run/docker.sock:/var/run/docker.sock -v /home/jenkins/.jenkins/etc/group:/etc/group:ro --group-add 999 ')

        // Written files
        assert 'jenkins:x:1000:1000::/home/jenkins:/bin/sh' == actualWriteFileArgs['.jenkins/etc/passwd']
        assert actualDockerGroup == actualWriteFileArgs['.jenkins/etc/group']
    }

    @Test
    void imageMountJenkinsUserUnexpectedPasswd() {
        actualPasswd = 'jenkins:x:1000:1000'
        testForInvalidPassword(
                { image -> image.mountJenkinsUser() },
                '/etc/passwd entry for current user does not match user:x:uid:gid:')
    }

    @Test
    void imageMountJenkinsUserPasswdEmpty() {
        actualPasswd = ''
        testForInvalidPassword(
                { image -> image.mountJenkinsUser() },
                'Unable to parse user jenkins from /etc/passwd.')
    }

    @Test
    void imageMountDockerSocketPasswdEmpty() {
        actualDockerGroup = ''
        testForInvalidPassword(
                { image -> image.mountDockerSocket() },
                'Unable to parse group docker from /etc/group. Docker host will not be accessible for container.')
    }

    @Test
    void "install older docker clients"() {
        Docker docker = createWithImage(mockedImageMethodInside())

        def oldUrlVersions = [ '17.03.0', '17.03.1', '17.03.2', '17.06.0', '17.06.1', '17.06.2', '17.09.0', '17.09.1', 
                '17.12.0', '17.12.1', '18.03.0', '18.03.1', '18.06.0', '18.06.1', '18.06.2', '18.06.3']
        
        oldUrlVersions.forEach({
            actualShArgs = []
            docker.image(expectedImage)
                    .installDockerClient(it)
                    .inside { return 'expectedClosure' }
            assert actualShArgs[0].contains("${it}-ce.tgz")
        })
        
        // Also accept URLs ending in -ce
        oldUrlVersions.forEach({
            actualShArgs = []
            docker.image(expectedImage)
                    .installDockerClient("${it}-ce")
                    .inside { return 'expectedClosure' }
            assert actualShArgs[0].contains("${it}-ce.tgz")
        })
    }

    @Test
    void "install newer docker clients"() {
        Docker docker = createWithImage(mockedImageMethodInside())

        def oldUrlVersions = [ '18.09.0', '19.03.9']

        oldUrlVersions.forEach({
            actualShArgs = []
            docker.image(expectedImage)
                    .installDockerClient(it)
                    .inside { return 'expectedClosure' }
            assert actualShArgs[0].contains("${it}.tgz")
        })
    }

    @Test
    void "install docker clients for current server version"() {
        Docker docker = createWithImage(mockedImageMethodInside())
        docker.image(expectedImage)
                .installDockerClient()
                .inside { return 'expectedClosure' }
        assert actualShArgs[0].contains("${actualDockerServerVersion}.tgz")
    }
    
    @Test
    void "repo digest"() {
        def expectedDigest = "hello-world@sha256:7f0a9f93b4aa3022c3a4c147a449bf11e0941a1fd0bf4a8e6c9408b2600777c5"
        actualRepoDigests = expectedDigest + "\n\n"
        def digests = createWithImage().image(expectedImage).repoDigests()

        assert digests.size() == 1
        assert digests[0] == expectedDigest
    }
    
    @Test
    void "repo digest empty"() {
        actualRepoDigests = '\n'
        def digests = createWithImage().image(expectedImage).repoDigests()

        assert digests.size() == 0
    }
    
    @Test
    void "repo digest multiple"() {
        actualRepoDigests = "a\nb\nc\n\n"
        def digests = createWithImage().image(expectedImage).repoDigests()

        assert digests.size() == 3
        assert digests[0] == 'a'
        assert digests[1] == 'b'
        assert digests[2] == 'c'
    }
    
    @Test
    void "push image"() {
        createWithImage().image(expectedImage).push()
        assert actualPushParams == ['', null]
    }
    
    @Test
    void "push image with name"() {
        createWithImage().image(expectedImage).push('name')
        assert actualPushParams == ['name', null]
    }
    
    @Test
    void "push image with name and force"() {
        createWithImage().image(expectedImage).push('name', true)
        assert actualPushParams == ['name', true]
    }
    
    @Test
    void "tag image"() {
        createWithImage().image(expectedImage).tag()
        assert actualTagParams == ['', null]
    }
    
    @Test
    void "tag image with name"() {
        createWithImage().image(expectedImage).tag('name')
        assert actualTagParams == ['name', null]
    }
    
    @Test
    void "tag image with name and force"() {
        createWithImage().image(expectedImage).tag('name', true)
        assert actualTagParams == ['name', true]
    }

    private Docker create(Map<String, Closure> mockedMethod) {
        Map<String, Map<String, Closure>> mockedScript = [
                docker: mockedMethod
        ]
        return new Docker(mockedScript)
    }

    /**
     * @return Mock Docker instance with mock image, that contains mocked methods.
     */
    private Docker createWithImage(Map<String, Closure> mockedMethod = [:]) {
        def mockedScript = [
                docker: [image: { String id ->
                    assert id == expectedImage
                    mockedMethod.put('id', id)
                    mockedMethod.put('push', { String param1 = '', Boolean param2 = null -> actualPushParams = [param1, param2] })
                    mockedMethod.put('tag', { String param1 = '', Boolean param2 = null -> actualTagParams = [param1, param2] })
                    return mockedMethod
                    }
                ],
                sh: { args ->

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
                    if (script.contains('docker version --format \'{{.Server.Version}}\'')) return "  ${actualDockerServerVersion}  "    
                    if (script.contains('RepoDigests')) return "  ${actualRepoDigests}  "    
                    else fail("Unexpected sh call. Script: " + script)
                },
                pwd: { return expectedHome },
                writeFile: { Map<String, String> args -> actualWriteFileArgs.put(args['file'], args['text']) },
                error: { String arg -> throw new RuntimeException(arg) 
                },
                env: []
        ]

        return new Docker(mockedScript)
    }

    /**
     * @return a map that defines a run() method returning its params, to be used as param in createWithImage()}.
     */
    private def mockedImageMethodRun() {
        [run: { String param1, String param2 -> return [param1, param2] }]
    }
    
    /**
     * @return a map that defines an inside() method returning its params, to be used as param in createWithImage()}.
     */
    private def mockedImageMethodInside() {
        [inside: { String param1, Closure param2 -> return [param1, param2] }]
    }

    /**
     * @return a map that defines a withRun() method returning its params, to be used as param in createWithImage()}.
     */
    private def mockedImageMethodWithRun() {
        [withRun: { String param1, String param2, Closure param3 ->
            return [param1, param2, param3]
        }]
    }

    private void testForInvalidPassword(Closure imageHook, String expectedError) {
        Docker docker = createWithImage(mockedImageMethodRun())

        def exception = shouldFail {
            def image = docker.image(expectedImage)
            imageHook.call(image)
            image.run('arg', 'cmd')
        }

        assertEquals(expectedError, exception.getMessage())
    }
}
