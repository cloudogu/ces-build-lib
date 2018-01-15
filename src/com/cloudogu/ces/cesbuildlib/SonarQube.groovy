package com.cloudogu.ces.cesbuildlib

/**
 * Abstraction for SonarQube. Use in conjunction with the SonarQube plugin for Jenkins:
 *
 * https://wiki.jenkins.io/display/JENKINS/SonarQube+plugin and
 * https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins#AnalyzingwithSonarQubeScannerforJenkins-AnalyzinginaJenkinspipeline
 */
class SonarQube implements Serializable {
    def script

    String sonarQubeEnv
    // If enabled uses the branch plugin, available for developer edition and above
    boolean isUsingBranchPlugin = false
    boolean isIgnoringBranches = false
    private String gitHubRepoName = ""
    private String gitHubCredentials = ""

    SonarQube(script, String sonarQubeEnv) {
        this.script = script
        this.sonarQubeEnv = sonarQubeEnv
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
        initMaven(mvn)
        String sonarExtraProps = script.env.SONAR_EXTRA_PROPS
        if (sonarExtraProps == null) {
            sonarExtraProps = ""
        }

        script.withSonarQubeEnv(sonarQubeEnv) {
            mvn "${script.env.SONAR_MAVEN_GOAL} -Dsonar.host.url=${script.env.SONAR_HOST_URL} " +
                    "-Dsonar.login=${script.env.SONAR_AUTH_TOKEN} ${sonarExtraProps} " +
                    //exclude generated code in target folder in order to avoid duplicates and issues in code that cannot be changed.
                    "-Dsonar.exclusions=target/**"
        }
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
        boolean isQualityGateSucceeded = true
        // Pull Requests are analyzed locally, so no calling of the QGate webhook
        if (!script.isPullRequest()) {
            script.timeout(time: 2, unit: 'MINUTES') { // Needed when there is no webhook for example
                def qGate = script.waitForQualityGate()
                script.echo "SonarQube Quality Gate status: ${qGate.status}"
                if (qGate.status != 'OK') {
                    isQualityGateSucceeded = false
                }
            }
        }
        return isQualityGateSucceeded
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
        this.gitHubRepoName = new Git(script).gitHubRepositoryName
    }

    private initMaven(Maven mvn) {
        if (script.isPullRequest()) {
            initMavenForPullRequest(mvn)
        } else {
            initMavenForRegularAnalysis(mvn)
        }
    }

    private void initMavenForRegularAnalysis(Maven mvn) {
        script.echo "SonarQube analyzing branch ${script.env.BRANCH_NAME}"

        if (isIgnoringBranches) {
            return
        }

        // Run SQ analysis in specific project for feature, hotfix, etc.
        // Note that -Dsonar.branch is deprecated from SQ 6.6: https://docs.sonarqube.org/display/SONAR/Analysis+Parameters
        // However, the alternative (the branch plugin is paid version only)
        // See https://docs.sonarqube.org/display/PLUG/Branch+Plugin
        if (isUsingBranchPlugin) {
            mvn.additionalArgs += " -Dsonar.branch.name=${script.env.BRANCH_NAME} "
            if (!"master".equals(script.env.BRANCH_NAME)) {
                // Avoid exception "The main branch must not have a target" on master branch
                mvn.additionalArgs += "-Dsonar.branch.target=master "
            }
        } else {
            mvn.additionalArgs = "-Dsonar.branch=${script.env.BRANCH_NAME}"
        }
    }

    private void initMavenForPullRequest(Maven mvn) {
        script.echo "SonarQube analyzing PullRequest ${script.env.CHANGE_ID}. Using preview mode. "

        // See https://docs.sonarqube.org/display/PLUG/GitHub+Plugin
        mvn.additionalArgs = "-Dsonar.analysis.mode=preview "
        mvn.additionalArgs += "-Dsonar.github.pullRequest=${script.env.CHANGE_ID} "

        if (gitHubCredentials != null && !gitHubCredentials.isEmpty()) {
            mvn.additionalArgs += "-Dsonar.github.repository=$gitHubRepoName "
            script.withCredentials([script.string(credentialsId: gitHubCredentials, variable: 'PASSWORD')]) {
                mvn.additionalArgs += "-Dsonar.github.oauth=${script.env.PASSWORD} "
            }
        }
    }
}
