/**
 * shellcheck for jenkins-pipelines https://github.com/koalaman/shellcheck
 *
 */
package com.cloudogu.ces.cesbuildlib

import groovy.transform.Field

// renovate: datasource=docker depName=koalaman/shellcheck-alpine
@Field static final String DEFAULT_SHELLCHECK_VERSION = "v0.11.0"

/**
 * run shellcheck with a custom fileList
 * sample input "test1.sh", "test2.sh"
 *
 * @param version tag of the koalaman/shellcheck-alpine image, e.g. "v0.11.0"
 */
def call(fileList, String version = DEFAULT_SHELLCHECK_VERSION) {
    executeWithDocker(fileList, version)
}

/**
 * run shellcheck on every .sh file inside the project folder
 * note: it ignores ./ecosystem folder for usage with ecosystem instances
 *
 */
def call() {
        def fileList = sh (script: 'find . -path ./ecosystem -prune -o -type f -regex .*\\.sh -print', returnStdout: true)
        fileList='"' + fileList.trim().replaceAll('\n','" "') + '"'
        executeWithDocker(fileList, DEFAULT_SHELLCHECK_VERSION)
}

/*
* run the alpine based shellcheck image
* note: we encountered some problems while using the minified docker image
*/
private def executeWithDocker(fileList, String version){
    docker.image("koalaman/shellcheck-alpine:${version}").inside(){
        sh "/bin/shellcheck -P SCRIPTDIR ${fileList}"
    }
}
