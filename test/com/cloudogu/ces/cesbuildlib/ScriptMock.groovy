package com.cloudogu.ces.cesbuildlib

class ScriptMock {
    def env = [ WORKSPACE: "", HOME: "" ]

    boolean expectedIsPullRequest = false
    def expectedQGate
    def expectedPwd

    /** Used when no value in expectedShRetValueForScript matches **/
    def expectedDefaultShRetValue
    def expectedShRetValueForScript = [:]

    String actualSonarQubeEnv

    Map actualTimeoutParams = [:]

    List<Map> actualUsernamePasswordArgs = []
    List<String> actualShStringArgs = new LinkedList<>()
    List<String> actualEcho = new LinkedList<>()

    List<String> actualShMapArgs = new LinkedList<>()

    List<Map<String, String>> writeFileParams = new LinkedList<>()
    Map actualFileArgs
    Map actualStringArgs
    Map files = new HashMap<String, String>()
    List<String> actualWithEnv
    String actualDir
    def actualGitArgs

    String sh(String args) {
        actualShStringArgs.add(args.toString())
        return getReturnValueFor(args)
    }

    String sh(Map<String, Object> args) {
        actualShMapArgs.add(args.script.toString())
        return getReturnValueFor(args.get('script'))
    }

    private Object getReturnValueFor(Object arg) {
        if (expectedDefaultShRetValue == null) {
            // toString() to make Map also match GStrings
            def value = expectedShRetValueForScript.get(arg.toString())
            if (value instanceof List) {
                return ((List) value).removeAt(0)
            } else {
                return value
            }
        } else {
            return expectedDefaultShRetValue
        }
    }

    boolean isPullRequest() {
        return expectedIsPullRequest
    }

    def timeout(Map params, closure) {
        this.actualTimeoutParams = params
        return closure.call()
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

    def withCredentials(List args, Closure closure) {
        closure.call()
    }

    void usernamePassword(Map args) {
        actualUsernamePasswordArgs.add(args)
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

    void echo(String msg) {
        actualEcho.add(msg)
    }

    String pwd() { expectedPwd }

    def git(def args) {
        actualGitArgs = args
        return args
    }

    void writeFile(Map<String, String> params) {
        writeFileParams.add(params)
    }

    String readFile(String file) {
        return files.get(file)
    }

    void dir(String dir, Closure closure) {
        actualDir = dir
        closure.call()
    }


    Map<String, String> actualWithEnvAsMap() {
        actualWithEnv.collectEntries {[it.split('=')[0], it.split('=')[1]]}
    }
}
