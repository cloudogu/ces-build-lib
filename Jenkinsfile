#!groovy
@Library(['github.com/cloudogu/ces-build-lib@testing/testing_trivyignore'])
import com.cloudogu.ces.cesbuildlib.*

node('docker') {
    timestamps {
        Trivy trivy
        
        stage('Checkout') {
            checkout scm
        }
        
        stage('Pull image') {
            new Docker(this)
                .image("ubuntu:20.04")
                .inside {
                    sh "echo 'hello world'"
                }
        }
        
        stage('Scan image') {
            trivy = new Trivy(this, new Docker(this), "0.57.1")
            sh "ls -la"
            boolean trivyExitCode = trivy.scanImage("ubuntu:20.04", "${this.env.WORKSPACE}/trivy/trivyReport.json", "", TrivySeverityLevel.ALL, TrivyScanStrategy.FAIL)
        }

        stage('Save file as json') {
            trivy.saveFormattedTrivyReport(TrivyScanFormat.JSON)
        }

        stage('Save file as table') {
            trivy.saveFormattedTrivyReport(TrivyScanFormat.TABLE)
        }
        
        stage('Save file as html') {
            trivy.saveFormattedTrivyReport(TrivyScanFormat.HTML)
        }
    }
}

