package com.cloudogu.ces.cesbuildlib

/**
 * An HTTP client that calls curl on the shell.
 *
 * Returns a map of 
 * * httpCode (String)
 * * headers (Map)
 * * body (String)
 */
class HttpClient implements Serializable {
    private script
    private credentials
    private Sh sh

    HttpClient(script, credentials = '') {
        this.script = script
        this.credentials = credentials
        this.sh = new Sh(script)
    }

    Map get(String url, String contentType = '', def data = '') {
        return httpRequest('GET', url, contentType, data)
    }

    Map put(String url, String contentType = '', def data = '') {
        return httpRequest('PUT', url, contentType, data)
    }

    Map putFile(String url, String contentType = '', String filePath) {
        String command
        executeWithCredentials {
            command = getUploadFileCurlCommand('PUT', url, contentType, filePath)
        }
        return httpRequest('PUT', url, contentType, filePath, command)
    }

    Map post(String url, String contentType = '', def data = '') {
        return httpRequest('POST', url, contentType, data)
    }

    protected String executeWithCredentials(Closure closure) {
        if (credentials) {
            script.withCredentials([script.usernamePassword(credentialsId: credentials,
                passwordVariable: 'CURL_PASSWORD', usernameVariable: 'CURL_USER')]) {
                closure.call(true)
            }
        } else {
            closure.call(false)
        }
    }

    private static String escapeSingleQuotes(String toEscape) {
        return toEscape.replaceAll("'", "'\"'\"'")
    }

    protected String getCurlAuthParam() {
        "-u '" + escapeSingleQuotes(script.env.CURL_USER) + ":" + escapeSingleQuotes(script.env.CURL_PASSWORD) + "' "
    }

    private String getCurlCommand(String httpMethod, String url, String contentType, String data) {
        return "curl -i -X '" + escapeSingleQuotes(httpMethod) + "' " +
            (credentials ? getCurlAuthParam() : '') +
            (contentType ? "-H 'Content-Type: " + escapeSingleQuotes(contentType) + "' " : '') +
            (data ? "-d '" + escapeSingleQuotes(data) + "' " : '') +
            "'" + escapeSingleQuotes(url) + "'"
    }

    private String getUploadFileCurlCommand(String httpMethod, String url, String contentType, String filePath) {
        return "curl -i -X '" + escapeSingleQuotes(httpMethod) + "' " +
            (credentials ? getCurlAuthParam() : '') +
            (contentType ? "-H 'Content-Type: " + escapeSingleQuotes(contentType) + "' " : '') +
            (filePath ? "-T '" + escapeSingleQuotes(filePath) + "' " : '') +
            "'" + escapeSingleQuotes(url) + "'"
    }

    protected Map httpRequest(String httpMethod, String url, String contentType, def data, String customCommand = '') {
        String httpResponse
        def rawHeaders
        def body

        executeWithCredentials {
            String curlCommand
            if (customCommand.isEmpty()) {
                curlCommand = getCurlCommand(httpMethod, url, contentType, data.toString())
            } else {
                curlCommand = customCommand
            }

            // Command must be run inside this closure, otherwise the credentials will not be masked (using '*') in the console
            httpResponse = sh.returnStdOut curlCommand
        }

        String[] responseLines = httpResponse.split("\n")

        // e.g. HTTP/2 301
        String httpCode = responseLines[0].split(" ")[1]
        def separatingLine = responseLines.findIndexOf { it.trim().isEmpty() }

        if (separatingLine > 0) {
            rawHeaders = responseLines[1..(separatingLine - 1)]
            body = responseLines[separatingLine + 1..-1].join('\n')
        } else {
            // No body returned
            rawHeaders = responseLines[1..-1]
            body = ''
        }

        def headers = [:]
        for (String line : rawHeaders) {
            // e.g. cache-control: no-cache
            def splitLine = line.split(':', 2)
            headers[splitLine[0].trim()] = splitLine[1].trim()
        }
        return [httpCode: httpCode, headers: headers, body: body]
    }
}
