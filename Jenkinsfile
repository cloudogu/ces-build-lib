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

    def mvnHome = tool 'M3'
    def javaHome = tool 'JDK8'

    Maven mvn = new Maven(this, mvnHome, javaHome)
    Git git = new Git(this)

    String emailRecipients = env.EMAIL_RECIPIENTS

    catchError {
        stage('Checkout') {
            checkout scm
            git.clean("")
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

    // email on fail
    step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailRecipients, sendToIndividuals: true])
}