#!groovy

@Library('ces-build-lib@develop')
import com.cloudogu.ces.buildlib.*

node() {

    properties([
            // Keep only the last 10 build to preserve space
            //buildDiscarder(logRotator(numToKeepStr: '10')),
            [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', numToKeepStr: '10']],
            // Don't run concurrent builds for a branch, because they use the same workspace directory
            disableConcurrentBuilds()
    ])

    def mvnHome = tool 'M3.3'
    def javaHome = tool 'JDK8'

    Maven mvn = new Maven(this, mvnHome, javaHome)


    String emailRecipients = env.EMAIL_RECIPIENTS

    catchError {
        stage('Checkout') {
            checkout scm
        }

        stage('Build') {
            // Run the maven build
            mvn 'clean install -DskipTests'
        }

        stage('Build') {
            // Run the maven build
            mvn 'clean install -DskipTests'
            archive '**/target/*.jar,**/target/*.zip'
        }

        //parallel unitTests: {
        stage('Unit Test') {
            mvn 'test'
        }
    }

    // Archive Unit and integration test results, if any
    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml,**/target/surefire-reports/TEST-*.xml'

    // email on fail
    step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: emailRecipients, sendToIndividuals: true])
}