#!groovy

node('docker') {

    properties([
            // Keep only the last 10 build to preserve space
            buildDiscarder(logRotator(numToKeepStr: '10')),
            // Don't run concurrent builds for a branch, because they use the same workspace directory
            disableConcurrentBuilds()
    ])

    def cesBuildLib = libraryFromLocalRepo().com.cloudogu.ces.cesbuildlib

    def mvn = cesBuildLib.MavenWrapperInDocker.new(this, 'adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine')
    mvn.useLocalRepoFromJenkins = true
    def git = cesBuildLib.Git.new(this)

    if ("master".equals(env.BRANCH_NAME)) {
        mvn.additionalArgs = "-DperformRelease"
        currentBuild.description = mvn.getVersion()
    }

    String emailRecipients = env.EMAIL_RECIPIENTS

    catchError {
        stage('Checkout') {
            checkout scm
            /* Don't remove folders starting in "." like
             * .m2 (maven)
             * .npm
             * .cache, .local (bower)
             */
            git.clean('".*/"')
        }

        stage('Build') {
            // Run the maven build
            mvn 'clean install -DskipTests'
            archive 'target/*.jar'
        }

        stage('Unit Test') {
            mvn 'test'
            // Archive Unit and integration test results, if any
            junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml'
        }

        stage('SonarQube') {
            generateCoverageReportForSonarQube(mvn)
            def sonarQube = cesBuildLib.SonarQube.new(this, 'ces-sonar')
            sonarQube.updateAnalysisResultOfPullRequestsToGitHub('sonarqube-gh-token')

            sonarQube.analyzeWith(mvn)

            if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
                unstable("Pipeline unstable due to SonarQube quality gate failure")
            }
        }
    }

    mailIfStatusChanged(findEmailRecipients(emailRecipients))
}

static void generateCoverageReportForSonarQube(def mvn) {
    mvn 'org.jacoco:jacoco-maven-plugin:0.8.5:report'
}

def libraryFromLocalRepo() {
    // Workaround for loading the current repo as shared build lib.
    // Checks out to workspace local folder named like the identifier.
    // We have to pass an identifier with version (which is ignored). Otherwise the build fails.
    library(identifier: 'ces-build-lib@snapshot', retriever: legacySCM(scm))
}
