package com.cloudogu.ces.cesbuildlib

// TODO docs in .txt and README
//findVulnerabilitiesWithTrivy([ imageName: 'nginx', severities=[ 'HIGH, CRITICAL' ], trivyVersion: '0.15.0'])
def call (Map args) {
    def imageName = args.imageName 
    if (!imageName) {
        error 'No image name passed to findVulnerabilitiesWithTrivy()'
    }
    def trivyVersion = args.trivyVersion ? args.trivyVersion : '0.15.0'
    def severityFlag = args.severity ? "--severity=${severity.join(',')}" : ''
    
    sh "mkdir -p .trivy/.cache"
    
    new Docker(this).image("aquasec/trivy:${trivyVersion}")
        .mountJenkinsUser()
        .mountDockerSocket()
        .inside("-v ${env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
            
            sh "trivy image -f json -o .trivy/trivyOutput.json ${severityFlag} ${imageName}"
    }
    // TODO implement allowList

    def trivyOutput = readJSON file: "${env.WORKSPACE}/.trivy/trivyOutput.json"

    if(trivyOutput[0].Vulnerabilities.equals("null")) {
        return []
    } else {
        return trivyOutput[0].Vulnerabilities
    }
}
