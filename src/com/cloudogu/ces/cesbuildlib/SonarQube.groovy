package com.cloudogu.ces.cesbuildlib

/**
 * Abstraction for SonarQube. Use in conjunction with the SonarQube plugin for Jenkins:
 *
 * https://wiki.jenkins.io/display/JENKINS/SonarQube+plugin and
 * https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins#AnalyzingwithSonarQubeScannerforJenkins-AnalyzinginaJenkinspipeline
 */
class SonarQube implements Serializable {
    protected script

    // If enabled uses the branch plugin, available for developer edition and above
    boolean isUsingBranchPlugin = false
    boolean isIgnoringBranches = false
    private String gitHubRepoName = ""
    private String gitHubCredentials = ""
    protected Map config

    @Deprecated
    SonarQube(script, String sonarQubeEnv) {
        this(script, [sonarQubeEnv: sonarQubeEnv])
    }

    SonarQube(script, Map config) {
        this.script = script
        this.config = config
    }

    /**
     * Executes a SonarQube analysis using a SonarQube Scanner installed by Jenkins.
     *
     * When building a PullRequest, only a preview analysis is done. The result of this analysis can be added to the PR,
     * see {@link #updateAnalysisResultOfPullRequestsToGitHub(java.lang.String)}.
     *
     * The current branch name is added to the SonarQube project name. Paid versions of GitHub offer the branch plugin.
     * If available set {@link #isUsingBranchPlugin} to {@code true}.
     *
     */
    void analyzeWith(SonarQubeScanner scanner) {
        scanner.additionalArgs += createAnalysisArgs()
        determineAnalysisStrategy(scanner).execute()
    }

    /**
     * Executes a SonarQube analysis using maven.
     *
     * When building a PullRequest, only a preview analysis is done. The result of this analysis can be added to the PR,
     * see {@link #updateAnalysisResultOfPullRequestsToGitHub(java.lang.String)}.
     *
     * The current branch name is added to the SonarQube project name. Paid versions of GitHub offer the branch plugin.
     * If available set {@link #isUsingBranchPlugin} to {@code true}.
     *
     */
    void analyzeWith(Maven mvn) {
        mvn.additionalArgs += createAnalysisArgs()
        determineAnalysisStrategy(new MavenAnalysisTool(mvn)).execute()
    }

    /**
     * Blocks until a webhook is called on Jenkins that signalizes finished SonarQube QualityGate evaluation.
     *
     * It's good practice to execute this outside of a build executor/node, in order not to block it while waiting.
     * However, if the webhook is set in most cases the result will be returned in a couple of seconds.
     *
     * If there is no webhook or SonarQube does not respond within 2 minutes, the build fails.
     * So make sure to set up a webhook in SonarQube global administration or per project to
     * {@code <JenkinsInstance> /sonarqube-webhook/}.
     * See https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins
     *
     * If this build is a Pull Request, this method will not wait, because usually PRs are analyzed locally.
     * See https://docs.sonarqube.org/display/PLUG/GitHub+Plugin
     *
     * Will only work after {@link #analyzeWith(com.cloudogu.ces.cesbuildlib.Maven)} is called. This is how the
     * SonarQube plugin for Jenkins works...
     *
     * @return {@code true} if the result of the quality is 'OK' or if a Pull Request is built. Otherwise {@code false}.
     */
    boolean waitForQualityGateWebhookToBeCalled() {
        if (!config['sonarQubeEnv']) {
            script.error "waitForQualityGate will only work when using the SonarQube Plugin for Jenkins, via the 'sonarQubeEnv' parameter"
        }

        if (!script.isPullRequest()) {
            return doWaitForQualityGateWebhookToBeCalled()
        }
        return doWaitForPullRequestQualityGateWebhookToBeCalled()
    }

    protected boolean doWaitForPullRequestQualityGateWebhookToBeCalled() {
        // Pull Requests are analyzed locally, so no calling of the QGate webhook
        true
    }

    protected boolean doWaitForQualityGateWebhookToBeCalled() {
        script.timeout(time: 2, unit: 'MINUTES') { // Needed when there is no webhook for example
            def qGate = script.waitForQualityGate()
            script.echo "SonarQube Quality Gate status: ${qGate.status}"
            if (qGate.status != 'OK') {
                return false
            }
            return true
        }
    }

    /**
     * SonarQube can update the Build status of a commit within a PullRequest at GitHub.
     *
     * To do so, it needs a GitHub access token, which should be passed via a Jenkins credential.
     *
     * See https://docs.sonarqube.org/display/PLUG/GitHub+Plugin
     */
    void updateAnalysisResultOfPullRequestsToGitHub(String gitHubCredentials) {
        this.gitHubCredentials = gitHubCredentials
        this.gitHubRepoName = new Git(script).repositoryName
    }

    protected String createAnalysisArgs() {
        if (script.isPullRequest()) {
            createAnalysisArgsForPullRequest()
        } else {
            createAnalysisArgsForRegularAnalysis()
        }
    }

    protected String createAnalysisArgsForRegularAnalysis() {
        script.echo "SonarQube analyzing branch ${script.env.BRANCH_NAME}"
        String args = ''

        if (isIgnoringBranches) {
            return ''
        }

        // Run SQ analysis in specific project for feature, hotfix, etc.
        // Note that -Dsonar.branch is deprecated from SQ 6.6: https://docs.sonarqube.org/display/SONAR/Analysis+Parameters
        // However, the alternative (the branch plugin is paid version only)
        // See https://docs.sonarqube.org/display/PLUG/Branch+Plugin
        if (isUsingBranchPlugin) {
            args += " -Dsonar.branch.name=${script.env.BRANCH_NAME} "
            if (!"master".equals(script.env.BRANCH_NAME)) {
                // Avoid exception "The main branch must not have a target" on master branch
                args += " -Dsonar.branch.target=master "
            }
        } else if (script.env.BRANCH_NAME) {
            args += " -Dsonar.branch=${script.env.BRANCH_NAME} "
        }
        return args
    }

    protected String createAnalysisArgsForPullRequest() {
        script.echo "SonarQube analyzing PullRequest ${script.env.CHANGE_ID}. Using preview mode. "
        String args = ''

        // See https://docs.sonarqube.org/display/PLUG/GitHub+Plugin
        args += " -Dsonar.analysis.mode=preview"
        args += " -Dsonar.github.pullRequest=${script.env.CHANGE_ID} "

        if (gitHubCredentials != null && !gitHubCredentials.isEmpty()) {
            args += "-Dsonar.github.repository=$gitHubRepoName "
            script.withCredentials([script.string(credentialsId: gitHubCredentials, variable: 'PASSWORD')]) {
                args += "-Dsonar.github.oauth=${script.env.PASSWORD} "
            }
        }
        return args
    }

    protected void validateFieldPresent(Map config, String fieldKey) {
        if (!config[fieldKey]) {
            script.error "Missing required '${fieldKey}' parameter."
        }
    }

    protected AnalysisStrategy determineAnalysisStrategy(AnalysisTool analysisTool) {
        // If private may fail for SonarCloud with:
        // No signature of method: com.cloudogu.ces.cesbuildlib.SonarCloud.determineAnalysisStrategy() is applicable for argument types: () values: []

        if (config['sonarQubeEnv']) {
            return new EnvAnalysisStrategy(script, analysisTool, config['sonarQubeEnv'])

        } else if (config['token']) {
            validateMandatoryFieldsWithoutSonarQubeEnv()
            return new TokenAnalysisStrategy(script, analysisTool, config['token'], config['sonarHostUrl'])

        } else if (config['usernamePassword']) {
            validateMandatoryFieldsWithoutSonarQubeEnv()
            return new UsernamePasswordAnalysisStrategy(script, analysisTool, config['usernamePassword'],
                    config['sonarHostUrl'])

        } else {
            script.error "Requires either 'sonarQubeEnv', 'token' or 'usernamePassword' parameter."
        }
    }

    protected void validateMandatoryFieldsWithoutSonarQubeEnv() {
        validateFieldPresent(config, 'sonarHostUrl')
    }

    interface AnalysisTool {
        void setAdditionalArgs(String additionalArgs)
        void analyze(String sonarMavenGoal, String sonarHostUrl, String sonarLogin, String sonarExtraProps)
    }

    private static class MavenAnalysisTool implements AnalysisTool {

        Maven mvn

        MavenAnalysisTool(Maven mvn) {
            this.mvn = mvn
        }

        void setAdditionalArgs(String additionalArgs) {
            mvn.additionalArgs = additionalArgs
        }

        void analyze(String sonarMavenGoal, String sonarHostUrl, String sonarLogin, String sonarExtraProps) {
            //Using mvn(), i.e. implicit mvn.call() results in
            // groovy.lang.MissingMethodException: No signature of method: java.lang.Class.mvn() is applicable

            mvn.call("${sonarMavenGoal} -Dsonar.host.url=${sonarHostUrl} -Dsonar.login=${sonarLogin} ${sonarExtraProps}")
        }
    }

    private static abstract class AnalysisStrategy {

        def script
        protected AnalysisTool analysisTool

        AnalysisStrategy(script,  AnalysisTool analysisTool) {
            this.script = script
            this.analysisTool = analysisTool
        }

        abstract execute()
    }

    private static class EnvAnalysisStrategy extends AnalysisStrategy {

        String sonarQubeEnv

        EnvAnalysisStrategy(script, AnalysisTool analysisTool, String sonarQubeEnv) {
            super(script, analysisTool)
            this.sonarQubeEnv = sonarQubeEnv
        }

        def execute() {
            script.withSonarQubeEnv(sonarQubeEnv) {
                String sonarExtraProps = script.env.SONAR_EXTRA_PROPS
                if (sonarExtraProps == null) {
                    sonarExtraProps = ""
                }

                analysisTool.analyze(script.env.SONAR_MAVEN_GOAL, script.env.SONAR_HOST_URL, script.env.SONAR_AUTH_TOKEN,
                        sonarExtraProps)
            }
        }
    }

    private static class TokenAnalysisStrategy extends AnalysisStrategy {

        String token
        String host

        TokenAnalysisStrategy(script, AnalysisTool analysisTool, String tokenCredential, String host) {
            super(script, analysisTool)
            this.token = tokenCredential
            this.host = host
        }

        def execute() {
            script.withCredentials([script.string(credentialsId: token, variable: 'SONAR_AUTH_TOKEN')]) {
                analysisTool.analyze('sonar:sonar', host, script.env.SONAR_AUTH_TOKEN, '')
            }
        }
    }

    private static class UsernamePasswordAnalysisStrategy extends AnalysisStrategy {

        String usernameAndPasswordCredential
        String host

        UsernamePasswordAnalysisStrategy(script, AnalysisTool analysisTool, String usernameAndPasswordCredential, String host) {
            super(script, analysisTool)
            this.usernameAndPasswordCredential = usernameAndPasswordCredential
            this.host = host
        }

        def execute() {
            script.withCredentials([script.usernamePassword(credentialsId: usernameAndPasswordCredential,
                    passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                analysisTool.analyze('sonar:sonar', host, script.env.USERNAME,
                        "-Dsonar.password=${script.env.PASSWORD} ")
            }
        }
    }
}
