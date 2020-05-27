package com.cloudogu.ces.cesbuildlib

class ScriptMock {
    def env = [WORKSPACE: "", HOME: ""]

    boolean expectedIsPullRequest = false
    boolean unstable = false
    String unstableMsg = ""
    def expectedQGate
    def expectedPwd

    /** Used when no value in expectedShRetValueForScript matches **/
    def expectedDefaultShRetValue
    def expectedShRetValueForScript = [:]

    String actualSonarQubeEnv

    Map actualTimeoutParams = [:]

    List<Map> actualUsernamePasswordArgs = []
    List<String> actualShStringArgs = new LinkedList<>()
    List<String> allActualArgs = new LinkedList<>()
    List<String> actualEcho = new LinkedList<>()

    List<String> actualShMapArgs = new LinkedList<>()

    List<Map<String, String>> writeFileParams = new LinkedList<>()
    Map actualFileArgs
    Map actualStringArgs
    Map files = new HashMap<String, String>()
    List<String> actualWithEnv
    String actualDir
    def actualGitArgs
    private ignoreOutputFile

    public ScriptMock() {
        this(true)
    }

    public ScriptMock(boolean ignoreOutputFile) {
        // This flag was added to make testing more convenient because we don't need to check for this in most tests.
        // When set to true, the script command "cat output" and "rm -f output" will not be saved in output
        // Also the " > output" at the end of each git command will be removed.
        this.ignoreOutputFile = ignoreOutputFile
    }

    String sh(String args) {
        if (this.shouldAddToArgs(args)) {
            actualShStringArgs.add(this.convert(args))
            allActualArgs.add(this.convert(args))
        }
        return getReturnValueFor(args)
    }

    String sh(Map<String, Object> args) {
        def script = args.get('script')

        if (this.shouldAddToArgs(script)) {
            actualShMapArgs.add(this.convert(script))
            allActualArgs.add(this.convert(script))
        }

        return getReturnValueFor(script)
    }

    private String convert(args) {
        def converted = args.toString()

        if (!this.ignoreOutputFile || converted.indexOf(" > output") == -1) {
            return converted
        }

        return converted.substring(0, converted.indexOf(" > output"))
    }

    private boolean shouldAddToArgs(args) {
        if (!this.ignoreOutputFile) {
            return true
        }

        return !args.equals("rm -f output") && !args.equals("cat output")
    }

    private Object getReturnValueFor(Object arg) {

        if (expectedDefaultShRetValue == null) {
            // toString() to make Map also match GStrings
            def value = expectedShRetValueForScript.get(this.convert(arg))
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

    def unstable(String msg) {
        this.unstable = true
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
        actualWithEnv.collectEntries { [it.split('=')[0], it.split('=')[1]] }
    }
}
