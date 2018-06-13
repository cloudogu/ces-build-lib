package com.cloudogu.ces.cesbuildlib

class ScriptMock {
    def env = [ WORKSPACE: "", HOME: "" ]

    boolean expectedIsPullRequest = false
    def expectedQGate
    def expectedShRetValue
    def expectedPwd

    String actualSonarQubeEnv
    Map actualUsernamePasswordArgs
    List<String> actualShStringArgs = new LinkedList<>()
    List<Map<String,String>> actualShMapArgs = new LinkedList<>()
    List<Map<String, String>> writeFileParams = new LinkedList<>()
    Map actualFileArgs
    Map actualStringArgs
    Map files = new HashMap<String, String>();
    List<String> actualWithEnv

    String sh(String args) {
        actualShStringArgs.add(args)
        expectedShRetValue
    }

    String sh(Map<String, String> args) {
        actualShMapArgs.add(args)
        expectedShRetValue
    }

    boolean isPullRequest() {
        return expectedIsPullRequest
    }

    void timeout(Map params, closure) {
        closure.call()
    }

    def waitForQualityGate() {
        return expectedQGate
    }

    void withSonarQubeEnv(String sonarQubeEnv, Closure closure) {
        this.actualSonarQubeEnv = sonarQubeEnv
        closure.call()
    }

    void withEnv(List<String> env, Closure closure) {
        this.actualWithEnv = env
        closure.call()
    }

    void withCredentials(List args, Closure closure) {
        closure.call()
    }

    void usernamePassword(Map args) {
        actualUsernamePasswordArgs = args
    }

    void file(Map args) {
        actualFileArgs = args
    }

    void string(Map args) {
        actualStringArgs = args
    }

    void error(String args) {
        throw new RuntimeException(args)
    }

    void echo(String msg) {}

    String pwd() { expectedPwd }

    def git(def args) {
        return args
    }

    void writeFile(Map<String, String> params) {
        writeFileParams.add(params)
    }

    String readFile(String file) {
        return files.get(file)
    }

    Map<String, String> actualWithEnvAsMap() {
        actualWithEnv.collectEntries {[it.split('=')[0], it.split('=')[1]]}
    }
}
