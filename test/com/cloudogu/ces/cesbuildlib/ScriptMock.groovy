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
    Map actualUsernamePasswordArgs
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
        if (expectedDefaultShRetValue == null) {
            // toString() to make Map also match GStrings
            return expectedShRetValueForScript.get(args.toString())
        } else {
            return expectedDefaultShRetValue
        }
    }

    String sh(Map<String, String> args) {
        // toString() to make Map also match GStrings
        actualShMapArgs.add(args.script.toString())
        if (expectedDefaultShRetValue == null) {
            return expectedShRetValueForScript.get(args.get('script').toString())
        } else {
            return expectedDefaultShRetValue
        }
    }

    boolean isPullRequest() {
        return expectedIsPullRequest
    }

    def timeout(Map params, closure) {
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
