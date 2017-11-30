package com.cloudogu.ces.cesbuildlib

/**
 * Abstraction for SonarQube. Use in conjunction with the SonarQube plugin for Jenkins:
 * https://wiki.jenkins.io/display/JENKINS/SonarQube+plugin and
 * https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins#AnalyzingwithSonarQubeScannerforJenkins-AnalyzinginaJenkinspipeline
 */
class SonarQube implements Serializable {
    def script

    String sonarQubeEnv

    SonarQube(script, String sonarQubeEnv) {
        this.script = script
        this.sonarQubeEnv = sonarQubeEnv
    }

    void analyzeWith(Maven mvn) {
        script.withSonarQubeEnv(sonarQubeEnv) {
            mvn "${script.SONAR_MAVEN_GOAL} -Dsonar.host.url=${script.SONAR_HOST_URL} " +
                    "-Dsonar.login=${script.SONAR_AUTH_TOKEN} ${script.SONAR_EXTRA_PROPS} " +
                    //exclude generated code in target folder in order to avoid duplicates and issues in code that cannot be changed.
                    "-Dsonar.exclusions=target/**"
        }
    }

    /**
     * Blocks until a webhook is called on Jenkins that signalizes finished SonarQube QualityGate evaluation.
     * If the Quality Gate fails the build status is set to {@code buildResultOnQualityGateFailure}.
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
    boolean waitForQualityGate() {
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
}
