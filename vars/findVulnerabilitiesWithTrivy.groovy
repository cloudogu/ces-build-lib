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
        def trivyVersion = args.trivyVersion ? args.trivyVersion : '0.57.1'
        def severityFlag = args.severity ? "${args.severity.join(',')}" : ''
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
    Trivy trivy = new Trivy(this, trivyVersion)
    trivy.scanImage(imageName, severityFlag, TrivyScanStrategy.UNSTABLE, additionalFlags, "${env.WORKSPACE}/.trivy/trivyOutput.json")

    def trivyOutput = readJSON file: "${env.WORKSPACE}/.trivy/trivyOutput.json"

    def vulnerabilities = []
    for (int i = 0; i < trivyOutput.Results.size(); i++) {

        if(trivyOutput.Results[i].Vulnerabilities != null ) {
            vulnerabilities += trivyOutput.Results[i].Vulnerabilities
        }
    }
    return vulnerabilities

}

static boolean validateArgs(Map args) {
    return !(args == null || args.imageName == null || args.imageName == '')
}
