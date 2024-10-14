package com.cloudogu.ces.cesbuildlib

//findVulnerabilitiesWithTrivy([ imageName: 'nginx', severity=[ 'HIGH, CRITICAL' ], additionalFlags: '--ignore-unfixed' ,trivyVersion: '0.41.0' ])
// Use a .trivyignore file for allowed CVEs
// If no vulnerabilities are found or no imageName was passed an empty List is returned
// Otherwise the list with all vulnerabilities (excluding the ones in the .trivyignore if one was passed)
ArrayList call (Map args) {
    //imageName is mandatory
    if(validateArgs(args)) {
        if(args.containsKey('allowList'))
            error "Arg allowList is deprecated, please use .trivyignore file"
        def imageName = args.imageName
        def trivyVersion = args.trivyVersion ? args.trivyVersion : '0.55.2'
        def severityFlag = args.severity ? "--severity=${args.severity.join(',')}" : ''
        def additionalFlags = args.additionalFlags ? args.additionalFlags : ''
        println(severityFlag)


        sh "mkdir -p .trivy/.cache"

        return getVulnerabilities(trivyVersion as String, severityFlag as String, additionalFlags as String, imageName as String)
    } else {
        error "There was no imageName to be processed. An imageName is mandatory to check for vulnerabilities."
        return []
    }
}

ArrayList getVulnerabilities(String trivyVersion, String severityFlag, String additionalFlags,String imageName) {
    // this runs trivy and creates an output file with found vulnerabilities
    runTrivyInDocker(trivyVersion, severityFlag, additionalFlags, imageName)

    def trivyOutput = readJSON file: "${env.WORKSPACE}/.trivy/trivyOutput.json"

    def vulnerabilities = []
    for (int i = 0; i < trivyOutput.Results.size(); i++) {

        if(trivyOutput.Results[i].Vulnerabilities != null ) {
            vulnerabilities += trivyOutput.Results[i].Vulnerabilities
        }
    }
    return vulnerabilities

}




def runTrivyInDocker(String trivyVersion, severityFlag, additionalFlags, imageName) {
    new Docker(this).image("aquasec/trivy:${trivyVersion}")
        .mountJenkinsUser()
        .mountDockerSocket()
        .inside("-v ${env.WORKSPACE}/.trivy/.cache:/root/.cache/") {

            sh "trivy image -f json -o .trivy/trivyOutput.json ${severityFlag} ${additionalFlags} ${imageName}"
        }
}



static boolean validateArgs(Map args) {
    return !(args == null || args.imageName == null || args.imageName == '')
}
