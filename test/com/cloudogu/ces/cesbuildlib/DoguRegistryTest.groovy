package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonSlurper
import org.junit.jupiter.api.Test
import static org.junit.jupiter.api.Assertions.*

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class DoguRegistryTest {

    @Test
    void testCreateRegistryObjectWithDefaults() {
        // when
        DoguRegistry sut = new DoguRegistry("script")

        // then
        assertTrue(sut != null)
    }

    @Test
    void testPushDogu() {
        // given
        String doguPath = "dogu.json"
        String doguStr = '{"Name": "testing/ldap", "Version": "2.4.48-4"}'
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.jsonFiles.put(doguPath, new JsonSlurper().parseText(doguStr))
        scriptMock.expectedShRetValueForScript.put("cat ${doguPath}".toString(), doguStr)

        def httpMock = mock(HttpClient.class)
        when(httpMock.put('http://url.de/api/v2/dogus/testing/ldap', 'application/json', doguStr)).then({ invocation ->
            return [
                httpCode: '200',
                body    : 'td'
            ]
        })

        DoguRegistry sut = new DoguRegistry(scriptMock, "http://url.de")
        sut.doguRegistryHttpClient = httpMock

        // when
        sut.pushDogu(doguPath)

        // then
        assertEquals("echo 'Push Dogu:\n-Namespace/Name: testing/ldap\n-Version: 2.4.48-4'", scriptMock.allActualArgs.get(0))
        assertEquals("cat dogu.json", scriptMock.allActualArgs.get(1))
    }

    @Test
    void testPushDoguWithImage() {
        // given
        String doguPath = "export/dogu.json"
        String doguStr = '{"Name": "official/cockpit", "Image": "registry.cloudogu.com/official/cockpit", "Version": "2.5.0-2"}'
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedPwd = "/ws@tmp"
        scriptMock.jsonFiles.put(doguPath, new JsonSlurper().parseText(doguStr))
        scriptMock.expectedShRetValueForScript.put("cat ${doguPath}".toString(), doguStr)

        def httpMock = mock(HttpClient.class)
        when(httpMock.put('https://dogu.cloudogu.com/api/v2/dogus/official/cockpit', 'application/json', doguStr)).then({ invocation ->
            return [httpCode: '200', body: '']
        })

        DoguRegistry sut = new DoguRegistry(scriptMock, "https://dogu.cloudogu.com", "harborrobotprerelease")
        sut.doguRegistryHttpClient = httpMock

        // when
        sut.pushDoguWithImage("export")

        // then
        assertEquals("harborrobotprerelease", scriptMock.actualUsernamePasswordArgs[0].credentialsId)
        assertEquals(["DOCKER_CONFIG=/ws@tmp/docker-push-config",
                      "IMAGE_REGISTRY=registry.cloudogu.com",
                      "IMAGE=registry.cloudogu.com/official/cockpit:2.5.0-2"], scriptMock.actualWithEnv.collect { it.toString() })
        assertEquals('echo "$REGISTRY_PASSWORD" | docker login "$IMAGE_REGISTRY" -u "$REGISTRY_USER" --password-stdin', scriptMock.allActualArgs.get(0))
        assertEquals('docker push "$IMAGE"', scriptMock.allActualArgs.get(1))
        assertEquals("cat export/dogu.json", scriptMock.allActualArgs.get(3))
    }

    @Test
    void testExitOnHttpErrorJson() {
        // given
        String doguPath = "dogu.json"
        String doguStr = '{"Name": "testing/ldap", "Version": "2.4.48-4"}'
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.jsonFiles.put(doguPath, new JsonSlurper().parseText(doguStr))
        scriptMock.expectedShRetValueForScript.put("cat ${doguPath}".toString(), doguStr)

        def httpMock = mock(HttpClient.class)
        when(httpMock.put('http://url.de/api/v2/dogus/testing/ldap', 'application/json', doguStr)).then({ invocation ->
            return [
                httpCode: '500',
                body    : 'body'
            ]
        })

        DoguRegistry sut = new DoguRegistry(scriptMock, "http://url.de")
        sut.doguRegistryHttpClient = httpMock

        // when
        sut.pushDogu(doguPath)

        // then
        assertEquals("echo 'Push Dogu:\n-Namespace/Name: testing/ldap\n-Version: 2.4.48-4'", scriptMock.allActualArgs.get(0))
        assertEquals("cat dogu.json", scriptMock.allActualArgs.get(1))
        assertEquals("echo 'Error pushing ${doguPath}'".toString(), scriptMock.allActualArgs.get(2))
        assertEquals("echo 'body'", scriptMock.allActualArgs.get(3))
        assertEquals("exit 1", scriptMock.allActualArgs.get(4))
    }

    @Test
    void testPushYaml() {
        // given
        String yamlPath = "path.yaml"
        String k8sName = "dogu-operator"
        String namespace = "testing"
        String version = "1.0.0"
        ScriptMock scriptMock = new ScriptMock()

        def httpMock = mock(HttpClient.class)
        when(httpMock.putFile('http://url.de/api/v1/k8s/testing/dogu-operator/1.0.0', 'application/yaml', yamlPath)).then({ invocation ->
            return [
                "httpCode": '200',
                "body"    : 'td'
            ]
        })

        DoguRegistry sut = new DoguRegistry(scriptMock, "http://url.de")
        sut.doguRegistryHttpClient = httpMock

        // when
        sut.pushK8sYaml(yamlPath, k8sName, namespace, version)

        // then
        assertEquals("echo 'Push Yaml:\n-Name: ${k8sName}\n-Namespace: ${namespace}\n-Version: ${version}'".toString(), scriptMock.allActualArgs.get(0))
    }

    @Test
    void testExitOnHttpErrorYaml() {
        // given
        String yamlPath = "path.yaml"
        String k8sName = "dogu-operator"
        String namespace = "testing"
        String version = "1.0.0"
        ScriptMock scriptMock = new ScriptMock()

        def httpMock = mock(HttpClient.class)
        when(httpMock.putFile('http://url.de/api/v1/k8s/testing/dogu-operator/1.0.0', 'application/yaml', yamlPath)).then({ invocation ->
            return [
                "httpCode": '491',
                "body"    : 'body'
            ]
        })

        DoguRegistry sut = new DoguRegistry(scriptMock, "http://url.de")
        sut.doguRegistryHttpClient = httpMock

        // when
        sut.pushK8sYaml(yamlPath, k8sName, namespace, version)

        // then
        assertEquals("echo 'Push Yaml:\n-Name: ${k8sName}\n-Namespace: ${namespace}\n-Version: ${version}'".toString(), scriptMock.allActualArgs.get(0))
        assertEquals("echo 'Error pushing ${yamlPath}'".toString(), scriptMock.allActualArgs.get(1))
        assertEquals("echo 'body'", scriptMock.allActualArgs.get(2))
        assertEquals("exit 1", scriptMock.allActualArgs.get(3))
    }
}
