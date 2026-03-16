package com.cloudogu.ces.cesbuildlib

/**
 * Abstraction for SonarQube. Use in conjunction with the SonarQube plugin for Jenkins:
 *
 * https://wiki.jenkins.io/display/JENKINS/SonarQube+plugin and
 * https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins#AnalyzingwithSonarQubeScannerforJenkins-AnalyzinginaJenkinspipeline
 */
class SonarQube implements Serializable {
    protected script

    boolean isIgnoringBranches = false
    int timeoutInMinutes = 2
    // If enabled uses the branch plugin, available for developer edition and above
    protected boolean isUsingBranchPlugin = false
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
     * Executes a SonarQube analysis using maven.
     *
     * The current branch name is added to the SonarQube project name. Paid versions of GitHub offer the branch plugin.
     * If available set {@link #isUsingBranchPlugin} to {@code true}.
     *
     */
    void analyzeWith(Maven mvn) {
        initMaven(mvn)
        determineAnalysisStrategy().executeWith(mvn)
    }

    /**
     * Blocks until a webhook is called on Jenkins that signalizes finished SonarQube QualityGate evaluation.
     *
     * It's good practice to execute this outside of a build executor/node, in order not to block it while waiting.
     * However, if the webhook is set in most cases the result will be returned in a couple of seconds.
     *
     * If there is no webhook or SonarQube does not respond within 2 minutes, the build fails.
     * So make sure to set up a webhook in SonarQube global administration or per project to
     * {@code <JenkinsInstance>/sonarqube-webhook/}.
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

        return doWaitForQualityGateWebhookToBeCalled()
    }

    protected boolean doWaitForQualityGateWebhookToBeCalled() {
        script.timeout(time: timeoutInMinutes, unit: 'MINUTES') { // Needed when there is no webhook for example
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
    @Deprecated
    void updateAnalysisResultOfPullRequestsToGitHub(String gitHubCredentials) {
        script.echo "WARNING: Decorating PRs was deprecated in SonarQube. See https://docs.sonarqube.org/display/PLUG/GitHub+Plugin"
        // As this is a deprecated method, the signature can't be changed. Still, the parameter is not needed. Ignore warnings. 
        // @SuppressWarnings(["grvy:org.codenarc.rule.unused.UnusedMethodParameterRule"]) does not seem to wirk
        gitHubCredentials
    }

    protected void initMaven(Maven mvn) {
        initMavenForRegularAnalysis(mvn)
    }

    protected void initMavenForRegularAnalysis(Maven mvn) {
        script.echo "SonarQube analyzing branch ${script.env.BRANCH_NAME}"

        if (isIgnoringBranches) {
            return
        }
        def artifactId = mvn.artifactId.trim()
        if (isUsingBranchPlugin) {
            mvn.additionalArgs += " -Dsonar.branch.name=${script.env.BRANCH_NAME} "

            String integrationBranch = determineIntegrationBranch()
            if (!integrationBranch.equals(script.env.BRANCH_NAME)) {
                String targetBranch = script.env.CHANGE_TARGET ? script.env.CHANGE_TARGET : integrationBranch
                // Avoid exception "The main branch must not have a target" on master branch
                mvn.additionalArgs += " -Dsonar.branch.target=${targetBranch} "
            }
            // Use -Dsonar.branch.name with following plugin:
            // https://github.com/mc1arke/sonarqube-community-branch-plugin
            // Some examples for Env Vars when building PRs.
            // BRANCH_NAME=PR-26
            // CHANGE_BRANCH=feature/simplify_git_push
            // CHANGE_TARGET=develop
        } else if (script.env.CHANGE_TARGET) {
            mvn.additionalArgs += "-Dsonar.projectKey=${replaceCharactersNotAllowedInProjectKey(artifactId)} " +
                " -Dsonar.projectName=${artifactId} " +
                " -Dsonar.pullrequest.key=${script.env.CHANGE_ID} " +
                " -Dsonar.pullrequest.branch=${script.env.CHANGE_BRANCH} " +
                " -Dsonar.pullrequest.base=${script.env.CHANGE_TARGET} "
        } else if (script.env.BRANCH_NAME) {
            mvn.additionalArgs += "-Dsonar.projectKey=${replaceCharactersNotAllowedInProjectKey(artifactId)} " +
                " -Dsonar.projectName=${artifactId} " +
                " -Dsonar.branch.name=${script.env.BRANCH_NAME} "
        }
    }

    protected static String replaceCharactersNotAllowedInProjectKey(String potentialProjectKey) {
        return potentialProjectKey.replaceAll("[^a-zA-Z0-9-_.:]", "_")
    }

    protected String determineIntegrationBranch() {
        if (config['integrationBranch']) {
            return config['integrationBranch']
        } else {
            return 'master'
        }
    }

    protected void validateFieldPresent(Map config, String fieldKey) {
        if (!config[fieldKey]) {
            script.error "Missing required '${fieldKey}' parameter."
        }
    }

    protected AnalysisStrategy determineAnalysisStrategy() {
        // If private may fail for SonarCloud with:
        // No signature of method: com.cloudogu.ces.cesbuildlib.SonarCloud.determineAnalysisStrategy() is applicable for argument types: () values: []

        if (config['sonarQubeEnv']) {
            return new EnvAnalysisStrategy(script, config['sonarQubeEnv'])

        } else if (config['token']) {
            validateMandatoryFieldsWithoutSonarQubeEnv()
            return new TokenAnalysisStrategy(script, config['token'], config['sonarHostUrl'])

        } else if (config['usernamePassword']) {
            validateMandatoryFieldsWithoutSonarQubeEnv()
            return new UsernamePasswordAnalysisStrategy(script, config['usernamePassword'], config['sonarHostUrl'])

        } else {
            script.error "Requires either 'sonarQubeEnv', 'token' or 'usernamePassword' parameter."
        }
    }

    protected void validateMandatoryFieldsWithoutSonarQubeEnv() {
        validateFieldPresent(config, 'sonarHostUrl')
    }

    private static abstract class AnalysisStrategy {

        def script
        def useTokenAuth

        AnalysisStrategy(script, useTokenAuth=false) {
            this.script = script
            this.useTokenAuth = useTokenAuth
        }

        abstract executeWith(Maven mvn)

        protected analyzeWith(Maven mvn, String sonarMavenGoal, String sonarHostUrl, String sonarLogin,
                              String sonarExtraProps = '') {

            String sonarAuthProperty = "-Dsonar.login=${sonarLogin}"
            if (useTokenAuth) {
                sonarAuthProperty = "-Dsonar.token=${sonarLogin}"
            }

            mvn "${sonarMavenGoal} -Dsonar.host.url=${sonarHostUrl} ${sonarAuthProperty} ${sonarExtraProps}"
        }
    }

    private static class EnvAnalysisStrategy extends AnalysisStrategy {

        String sonarQubeEnv

        EnvAnalysisStrategy(script, String sonarQubeEnv) {
            super(script)
            this.sonarQubeEnv = sonarQubeEnv
        }

        def executeWith(Maven mvn) {
            script.withSonarQubeEnv(sonarQubeEnv) {
                String sonarExtraProps = script.env.SONAR_EXTRA_PROPS
                if (sonarExtraProps == null) {
                    sonarExtraProps = ""
                }

                analyzeWith(mvn, script.env.SONAR_MAVEN_GOAL, script.env.SONAR_HOST_URL, script.env.SONAR_AUTH_TOKEN,
                    sonarExtraProps)
            }
        }
    }

    private static class TokenAnalysisStrategy extends AnalysisStrategy {

        String token
        String host

        TokenAnalysisStrategy(script, String tokenCredential, String host) {
            super(script, true)
            this.token = tokenCredential
            this.host = host
        }

        def executeWith(Maven mvn) {
            script.withCredentials([script.string(credentialsId: token, variable: 'SONAR_AUTH_TOKEN')]) {
                analyzeWith(mvn, 'sonar:sonar', host, script.env.SONAR_AUTH_TOKEN)
            }
        }
    }

    private static class UsernamePasswordAnalysisStrategy extends AnalysisStrategy {

        String usernameAndPasswordCredential
        String host

        UsernamePasswordAnalysisStrategy(script, String usernameAndPasswordCredential, String host) {
            super(script)
            this.usernameAndPasswordCredential = usernameAndPasswordCredential
            this.host = host
        }

        def executeWith(Maven mvn) {
            script.withCredentials([script.usernamePassword(credentialsId: usernameAndPasswordCredential,
                passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                analyzeWith(mvn, 'sonar:sonar', host, script.env.USERNAME,
                    "-Dsonar.password=${script.env.PASSWORD} ")
            }
        }
    }
}
