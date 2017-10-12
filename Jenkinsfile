#!groovy
@Library('github.com/cloudogu/ces-build-lib@develop')
import com.cloudogu.ces.cesbuildlib.*

node() {

    properties([
            // Keep only the last 10 build to preserve space
            buildDiscarder(logRotator(numToKeepStr: '10')),
            // Don't run concurrent builds for a branch, because they use the same workspace directory
            disableConcurrentBuilds()
    ])

    def sonarQube = 'ces-sonar'

    Maven mvn = new MavenInDocker(this, "3.5.0-jdk-8")
    Git git = new Git(this)

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
            archive '**/target/*.jar'
        }

        stage('Unit Test') {
            mvn 'test'
        }

        stage('SonarQube') {
            withSonarQubeEnv(sonarQube) {
                mvn "$SONAR_MAVEN_GOAL -Dsonar.host.url=$SONAR_HOST_URL " +
                        //exclude generated code in target folder
                        "-Dsonar.exclusions=target/**"
            }
        }
    }

    // Archive Unit and integration test results, if any
    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml'

    mailIfStatusChanged(emailRecipients)
}