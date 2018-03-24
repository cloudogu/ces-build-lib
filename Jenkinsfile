#!groovy

node('docker') {

    properties([
            // Keep only the last 10 build to preserve space
            buildDiscarder(logRotator(numToKeepStr: '10')),
            // Don't run concurrent builds for a branch, because they use the same workspace directory
            disableConcurrentBuilds()
    ])

    def cesBuildLib = libraryFromLocalRepo().com.cloudogu.ces.cesbuildlib

    def mvn = cesBuildLib.MavenInDocker.new(this, "3.5.0-jdk-8")
    mvn.useLocalRepoFromJenkins = true
    def git = cesBuildLib.Git.new(this)

    // TODO refactor this in an object-oriented way and move to build-lib
    if ("master".equals(env.BRANCH_NAME)) {
        mvn.additionalArgs = "-DperformRelease"
        currentBuild.description = mvn.getVersion()
    } else if (!"develop".equals(env.BRANCH_NAME)) {
        // run SQ analysis in specific project for feature, hotfix, etc.
        mvn.additionalArgs = "-Dsonar.branch=" + env.BRANCH_NAME
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
        }

        stage('Integration Test') {
            runIntegrationTests()
        }

        stage('SonarQube') {

            def sonarQube = cesBuildLib.SonarQube.new(this, 'ces-sonar')
            sonarQube.updateAnalysisResultOfPullRequestsToGitHub('cesmarvin')

            sonarQube.analyzeWith(mvn)

            if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
                currentBuild.result = 'UNSTABLE'
            }
        }
    }

    // Archive Unit and integration test results, if any
    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml'

    mailIfStatusChanged(emailRecipients)
}

def libraryFromLocalRepo() {
    // Workaround for loading the current repo as shared build lib.
    // Checks out to workspace local folder named like the identifier.
    // We have to pass an identifier with version (which is ignored). Otherwise the build fails.
    library(identifier: 'ces-build-lib@snapshot', retriever: legacySCM(scm))
}

void runIntegrationTests() {

    runIntegrationTest('Sh')
    // TODO how can we mount all files from MavenWrapper folders to /workspace while keeping the build lib?
    //runIntegrationTest('MavenWrapper')
}

void runIntegrationTest(String integrationTestFolder) {
    timeout(time: 2, unitintegrationTestFolder: 'MINUTES') { // Make sure to not wait forever for missing build executors, etc.
        // TODO mabe we should mount the current folder and set it as working dir?
        sh "docker run --rm " +
                // We need to mount the lib into /workspace so the libraryFromLocalRepo() workaround works
                "-v${pwd()}:/workspace " +
                // Now overwrite the lib's Jenkins file with the one from the test
                "-v${pwd()}/test/it/com/cloudogu/ces/cesbuildlib/$integrationTestFolder/Jenkinsfile:/workspace/Jenkinsfile " +
                "schnatterer/jenkinsfile-runner:1.0-SNAPSHOT-03d629a-jenkins2.108 "
    }
}