/**
 * shellcheck for jenkins-pipelines https://github.com/koalaman/shellcheck
 *
 */
package com.cloudogu.ces.cesbuildlib

/**
 * run shellcheck with a custom fileList
 * sample input "test1.sh", "test2.sh"
 *
 */
def call(fileList) {
    executeWithDocker(fileList)
}

/**
 * run shellcheck on every .sh file inside the project folder
 * note: it ignores ./ecosystem folder for usage with ecosystem instances
 *
 */
def call() {
        def fileList = sh (script: 'find . -path ./ecosystem -prune -o -type f -regex .*\\.sh -print', returnStdout: true)
        fileList='"' + fileList.trim().replaceAll('\n','" "') + '"'
        executeWithDocker(fileList)
}

/*
* run the alpine based shellcheck image
* note: we encountered some problems while using the minified docker image
*/
private def executeWithDocker(fileList){
    docker.image('koalaman/shellcheck-alpine:stable').inside(){
        sh "/bin/shellcheck ${fileList}"
    }
}
