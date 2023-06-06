package com.cloudogu.ces.cesbuildlib
//findVulnerabilitiesWithTrivy([ imageName: 'nginx', severity=[ 'HIGH, CRITICAL' ], trivyVersion: '0.41.0' ])
// Use a .trivyignore file for allowed CVEs
// If no vulnerabilities are found or no imageName was passed an empty List is returned
// Otherwise the list with all vulnerabilities (excluding the ones in the .trivyignore if one was passed)
ArrayList call (Map args) {

    if(validateArgs(args)) {
        if(args.containsKey('allowList'))
            error "Arg allowList is deprecated, please use .trivyignore file"
        def imageName = args.imageName
        def trivyVersion = args.trivyVersion ? args.trivyVersion : '0.41.0'
        def severityFlag = args.severity ? "--severity=${args.severity.join(',')}" : ''
        println(severityFlag)


        sh "mkdir -p .trivy/.cache"

        return getVulnerabilities(trivyVersion as String, severityFlag as String, imageName as String)
    } else {
        error "There was no imageName to be processed. An imageName is mandatory to check for vulnerabilities."
        return []
    }
}

ArrayList getVulnerabilities(String trivyVersion, String severityFlag, String imageName) {
    // this runs trivy and creates an output file with found vulnerabilities
    runTrivyInDocker(trivyVersion, severityFlag, imageName)

    def trivyOutput = readJSON file: "${env.WORKSPACE}/.trivy/trivyOutput.json"

    if(trivyOutput.Results[0].Vulnerabilities == null || trivyOutput.Results[0].Vulnerabilities.equals("null")) {
        return []
    } else {
        def vulnerabilities = trivyOutput.Results[0].Vulnerabilities as ArrayList
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

static boolean validateArgs(Map args) {
    return !(args == null || args.imageName == null || args.imageName == '')
}
