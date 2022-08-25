package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonSlurper
import org.junit.Test

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class DoguRegistryTest extends GroovyTestCase {

    @Test
    void testCreateRegistryObjectWithDefaults() {
        // given
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
        assertEquals("echo 'Error pushing ${doguPath}'", scriptMock.allActualArgs.get(2))
        assertEquals("echo 'body'", scriptMock.allActualArgs.get(3))
        assertEquals("exit 1", scriptMock.allActualArgs.get(4))
    }

    @Test
    void testPushYaml() {
        // given
        String yamlPath = "path.yaml"
        String yaml = "apiVersion: 1"
        String k8sName = "dogu-operator"
        String namespace = "testing"
        String version = "1.0.0"
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat ${yamlPath}".toString(), yaml)

        def httpMock = mock(HttpClient.class)
        when(httpMock.put('http://url.de/api/v1/k8s/testing/dogu-operator/1.0.0', 'application/yaml', yaml)).then({ invocation ->
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
        assertEquals("echo 'Push Yaml:\n-Name: ${k8sName}\n-Namespace: ${namespace}\n-Version: ${version}'", scriptMock.allActualArgs.get(0))
        assertEquals("cat path.yaml", scriptMock.allActualArgs.get(1))
    }

    @Test
    void testExitOnHttpErrorYaml() {
        // given
        String yamlPath = "path.yaml"
        String yaml = "apiVersion: 1"
        String k8sName = "dogu-operator"
        String namespace = "testing"
        String version = "1.0.0"
        ScriptMock scriptMock = new ScriptMock()
        scriptMock.expectedShRetValueForScript.put("cat ${yamlPath}".toString(), yaml)

        def httpMock = mock(HttpClient.class)
        when(httpMock.put('http://url.de/api/v1/k8s/testing/dogu-operator/1.0.0', 'application/yaml', yaml)).then({ invocation ->
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
        assertEquals("echo 'Push Yaml:\n-Name: ${k8sName}\n-Namespace: ${namespace}\n-Version: ${version}'", scriptMock.allActualArgs.get(0))
        assertEquals("cat path.yaml", scriptMock.allActualArgs.get(1))
        assertEquals("echo 'Error pushing ${yamlPath}'", scriptMock.allActualArgs.get(2))
        assertEquals("echo 'body'", scriptMock.allActualArgs.get(3))
        assertEquals("exit 1", scriptMock.allActualArgs.get(4))
    }
}
