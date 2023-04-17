package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonOutput
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class HttpClientTest {

    ScriptMock scriptMock = new ScriptMock()
    HttpClient http = new HttpClient(scriptMock)

    @Test
    void "simple request"() {
        def expectedResponse = 'HTTP/2 201\n' +
            'location: https://some:/url'
        scriptMock.expectedDefaultShRetValue = expectedResponse
        
        def actualResponse = http.get('http://url')

        assertThat(actualResponse.httpCode).isEqualTo('201')
        assertThat(actualResponse.headers['location']).isEqualTo('https://some:/url')
        assertThat(actualResponse.body).isEqualTo('')
        assertThat(scriptMock.actualShMapArgs[0]).isEqualTo('curl -i -X GET \'http://url\'')
    }

    @Test
    void "request with body"() {
        def expectedResponse = 'HTTP/1.1 203\n' +
            'cache-control: no-cache\n' +
            'content-type: output'
        scriptMock.expectedDefaultShRetValue = expectedResponse

        def dataJson = JsonOutput.toJson([
            title      : 't',
            description: 'd'
        ])

        def actualResponse = http.post('http://some-url', 'input', dataJson)

        assertThat(actualResponse.httpCode).isEqualTo('203')
        assertThat(actualResponse.headers['content-type']).isEqualTo('output')
        assertThat(actualResponse.body).isEqualTo('')

        assertThat(scriptMock.actualShMapArgs[0])
            .isEqualTo('curl -i -X POST -H \'Content-Type: input\' -d \'{"title":"t","description":"d"}\' \'http://some-url\'' )
    }

    @Test
    void "request with file upload"() {
        def expectedResponse = 'HTTP/1.1 203\n' +
            'cache-control: no-cache\n' +
            'content-type: output'
        scriptMock.expectedDefaultShRetValue = expectedResponse

        def actualResponse = http.putFile('http://some-url', 'input', "/path/to/file")

        assertThat(actualResponse.httpCode).isEqualTo('203')
        assertThat(actualResponse.headers['content-type']).isEqualTo('output')
        assertThat(actualResponse.body).isEqualTo('')

        assertThat(scriptMock.actualShMapArgs[0])
            .isEqualTo('curl -i -X PUT -H \'Content-Type: input\' -T \'/path/to/file\' \'http://some-url\'' )
    }

    @Test
    void "response with body"() {
        String expectedBody1 = '{"some":"body"}\n'
        String expectedBody2 = 'second line'
        def expectedResponse = 'HTTP/1.1 203\n' +
            'cache-control: no-cache\n' +
            'content-type: output\n' +
            '\n' +
            expectedBody1 +
            expectedBody2
        scriptMock.expectedDefaultShRetValue = expectedResponse

        def actualResponse = http.post('http://some-url')

        assertThat(actualResponse.httpCode).isEqualTo('203')
        assertThat(actualResponse.headers['content-type']).isEqualTo('output')
        assertThat(actualResponse.body).isEqualTo(expectedBody1 + expectedBody2)

        assertThat(scriptMock.actualShMapArgs[0])
            .isEqualTo('curl -i -X POST \'http://some-url\'' )
    }

    @Test
    void "request with credentials"() {
        http = new HttpClient(scriptMock, "credentialsID")
        scriptMock.env.put("CURL_USER", "user")
        scriptMock.env.put("CURL_PASSWORD", "pw")

        def expectedResponse = 'HTTP/2 201\n' +
            'location: https://some:/url'
        scriptMock.expectedDefaultShRetValue = expectedResponse
        
        http.get('http://url')

        assertThat(scriptMock.actualShMapArgs[0]).isEqualTo("curl -i -X GET -u 'user:pw' 'http://url'")
    }
}

