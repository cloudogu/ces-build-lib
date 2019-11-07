package com.cloudogu.ces.cesbuildlib

// Would make a wonderful inner class in SonarQube, but inner classes seem not to be supported.
// Results in "unable to resolve class SonarQube.Scanner"
class SonarQubeScanner implements Serializable, SonarQube.AnalysisTool {

    def script
    String additionalArgs = ""
    def scannerHome

    // e.g.     def scannerHome = tool name: 'sonar-scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
    SonarQubeScanner(script, scannerHome) {
        this.script = script
        this.scannerHome = scannerHome
    }

    void analyze(String sonarMavenGoal, String sonarHostUrl, String sonarLogin, String sonarExtraProps) {
        script.sh "${scannerHome}/bin/sonar-scanner -Dsonar.host.url=${sonarHostUrl} -Dsonar.login=${sonarLogin} ${sonarExtraProps} ${additionalArgs}"
    }
}
