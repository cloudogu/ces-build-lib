package com.cloudogu.ces.cesbuildlib

import java.util.function.BiConsumer
import java.util.function.Consumer

//findVulnerabilitiesWithTrivy([ imageName: 'nginx', severity=[ 'HIGH, CRITICAL' ], trivyVersion: '0.15.0', allowList: ['CVE-0000-0000, CVE-0000-0001'] ])
// If no vulnerabilities are found or no imageName was passed an empty List is returned
// Otherwise the list with all vulnerabilities (excluding the ones in the allowList if one was passed)
ArrayList call (Map args) {
    //imageName is mandatory
    if(validateArgs(args)) {
        def imageName = args.imageName
        def trivyVersion = args.trivyVersion ? args.trivyVersion : '0.15.0'
        def severityFlag = args.severity ? "--severity=${args.severity.join(',')}" : ''
        println(severityFlag)
        def allowList = args.allowList ? args.allowList : []

        sh "mkdir -p .trivy/.cache"

        return getVulnerabilities(trivyVersion as String, severityFlag as String, imageName as String, allowList as ArrayList)
    } else {
        error "There was no imageName to be processed. An imageName is mandatory to check for vulnerabilities."
        return []
    }
}

ArrayList getVulnerabilities(String trivyVersion, String severityFlag, String imageName, ArrayList allowList) {
    // this runs trivy and creates an output file with found vulnerabilities
    runTrivyInDocker(trivyVersion, severityFlag, imageName)

    def trivyOutput = readJSON file: "${env.WORKSPACE}/.trivy/trivyOutput.json"

    if(trivyOutput[0].Vulnerabilities == null || trivyOutput[0].Vulnerabilities.equals("null")) {
        return []
    } else {
        def vulnerabilities = filterAllowList(trivyOutput[0].Vulnerabilities as ArrayList, allowList as ArrayList)
        return vulnerabilities
    }
}

def runTrivyInDocker(String trivyVersion, severityFlag, imageName) {
    new Docker(this).image("aquasec/trivy:${trivyVersion}")
            .mountJenkinsUser()
            .mountDockerSocket()
            .inside("-v ${env.WORKSPACE}/.trivy/.cache:/root/.cache/") {

                sh "trivy image -f json -o .trivy/trivyOutput.json ${severityFlag} ${imageName}"
            }
}

static ArrayList filterAllowList(ArrayList vulnerabilities, ArrayList allowList) {
    if(allowList.isEmpty()) {
        return vulnerabilities
    }
    def filteredVulnerabilities = new ArrayList()
    vulnerabilities.forEach({ vuln ->
        def reportVulnerability = true
        allowList.forEach({ allow ->
            if(vuln.VulnerabilityID.equals(allow)){
                reportVulnerability = false
            }
        })
        if(reportVulnerability) {
            filteredVulnerabilities.add(vuln)
        }
    })
    return filteredVulnerabilities
}

static boolean validateArgs(Map args) {
    return !(args == null || args.imageName == null || args.imageName == '')
}
