package com.cloudogu.ces.cesbuildlib

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import static groovy.test.GroovyAssert.shouldFail
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class GpgTest {

    @Test
    void gpgCreateSignatureWithoutErrors() {

        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        ScriptMock scriptMock = new ScriptMock(dockerMock)
        scriptMock.expectedShRetValueForScript.put("whoami", "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")

        when(dockerMock.build(anyString(), anyString())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                scriptMock.sh("docker build call")
            }
        })
        when(dockerMock.image("cloudogu/gpg:1.0")).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser()).thenReturn(imageMock)
        when(imageMock.inside(ArgumentMatchers.eq("-v :/tmp/workspace --entrypoint='' -v null/.gnupg:/root/.gnupg"), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                scriptMock.sh("docker run call")
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })

        Gpg gpg = new Gpg(scriptMock, scriptMock.docker)
        gpg.createSignature()
        assert scriptMock.allActualArgs.size() == 8
        assert scriptMock.allActualArgs[0] == "docker build call"
        assert scriptMock.allActualArgs[1] == "rm -f Dockerfile.gpgbuild"
        assert scriptMock.allActualArgs[2] == "docker run call"
        assert scriptMock.allActualArgs[3] == "cd /tmp/workspace"
        assert scriptMock.allActualArgs[4] == "gpg --yes --always-trust --pinentry-mode loopback --passphrase=\"\$passphrase\" --import \$pkey"
        assert scriptMock.allActualArgs[5] == "make passphrase=\$passphrase signature-ci"
        assert scriptMock.allActualArgs[6] == "rm -f \$pkey"
        assert scriptMock.allActualArgs[7] == "rm -rf .gnupg"
        assert scriptMock.actualEcho.size() == 0
    }

    @Test
    void gpgCreateSignatureErrorOnImportKey() {
        Docker dockerMock = mock(Docker.class)
        Docker.Image imageMock = mock(Docker.Image.class)
        ScriptMock scriptMock = new ScriptMock(dockerMock)
        scriptMock.expectedShRetValueForScript.put("whoami", "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        scriptMock.expectedShCommandToThrow.put("gpg --yes --always-trust --pinentry-mode loopback --passphrase=\"\$passphrase\" --import \$pkey", new Exception("test-error"))

        when(dockerMock.build(anyString(), anyString())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                scriptMock.sh("docker build call")
            }
        })
        when(dockerMock.image("cloudogu/gpg:1.0")).thenReturn(imageMock)
        when(imageMock.mountJenkinsUser()).thenReturn(imageMock)
        when(imageMock.inside(ArgumentMatchers.eq("-v :/tmp/workspace --entrypoint='' -v null/.gnupg:/root/.gnupg"), any())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                scriptMock.sh("docker run call")
                Closure closure = invocation.getArgument(1)
                closure.call()
            }
        })

        Gpg gpg = new Gpg(scriptMock, scriptMock.docker)

        def exception = shouldFail {
            gpg.createSignature()
        }

        assert 'test-error' == exception.getMessage()

        assert scriptMock.allActualArgs.size() == 7
        println scriptMock.allActualArgs
        assert scriptMock.allActualArgs[0] == "docker build call"
        assert scriptMock.allActualArgs[1] == "rm -f Dockerfile.gpgbuild"
        assert scriptMock.allActualArgs[2] == "docker run call"
        assert scriptMock.allActualArgs[3] == "cd /tmp/workspace"
        assert scriptMock.allActualArgs[4] == "gpg --yes --always-trust --pinentry-mode loopback --passphrase=\"\$passphrase\" --import \$pkey"
        assert scriptMock.allActualArgs[5] == "rm -f \$pkey"
        assert scriptMock.allActualArgs[6] == "rm -rf .gnupg"
        assert scriptMock.actualEcho.size() == 1
        assert scriptMock.actualEcho[0] == "java.lang.Exception: test-error"
    }

    @Test
    void gpgCreateSignatureErrorOnDockerfileBuild() {
        Docker dockerMock = DockerMock.create()
        ScriptMock scriptMock = new ScriptMock(dockerMock)

        scriptMock.expectedShRetValueForScript.put("whoami", "jenkins")
        scriptMock.expectedShRetValueForScript.put("cat /etc/passwd | grep jenkins", "jenkins:x:1000:1000:jenkins,,,:/home/jenkins:/bin/bash")
        Docker.Image imageMock = mock(Docker.Image.class)
        when(dockerMock.build(anyString(), anyString())).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                scriptMock.sh("docker build call")
                throw new Exception("test-error")
            }
        })

        Gpg gpg = new Gpg(scriptMock, scriptMock.docker)

        def exception = shouldFail {
            gpg.createSignature()
        }

        assert 'test-error' == exception.getMessage()

        assert scriptMock.allActualArgs.size() == 4
        assert scriptMock.allActualArgs[0] == "docker build call"
        assert scriptMock.allActualArgs[1] == "rm -f Dockerfile.gpgbuild"
        assert scriptMock.allActualArgs[2] == "rm -f \$pkey"
        assert scriptMock.allActualArgs[3] == "rm -rf .gnupg"

        assert scriptMock.actualEcho.size() == 2
        assert scriptMock.actualEcho[0] == "java.lang.Exception: test-error"
        assert scriptMock.actualEcho[1] == "java.lang.Exception: test-error"
    }
}
