package com.cloudogu.ces.cesbuildlib

class Trivy implements Serializable {
    private static final String DEFAULT_TRIVY_VERSION = "0.57.0"
    private script
    private String trivyReportFilename
    private Docker docker

    Trivy(script, String trivyReportFilename = "${script.env.WORKSPACE}/.trivy/trivyReport.json", Docker docker = new Docker(script)) {
        this.script = script
        this.trivyReportFilename = trivyReportFilename
        this.docker = docker
    }

    /**
     * Scans an image for vulnerabilities.
     * Notes:
     * - Use a .trivyignore file for allowed CVEs
     * - This function will generate a JSON formatted report file which can be converted to other formats via saveFormattedTrivyReport()
     * - Evaluate via exit codes: 0 = no vulnerability; 1 = vulnerabilities found; other = function call failed
     *
     * @param imageName The name of the image to be scanned; may include a version tag
     * @param trivyVersion The version of Trivy used for scanning
     * @param additionalFlags Additional Trivy command flags
     * @param severityLevel The vulnerability level to scan. Can be a member of TrivySeverityLevel or a custom String (e.g. 'CRITICAL,LOW')
     * @param strategy The strategy to follow after the scan. Should the build become unstable or failed? Or Should any vulnerability be ignored? (@see TrivyScanStrategy)
     * @return Returns true if the scan was ok (no vulnerability found); returns false if any vulnerability was found
     */
    boolean scanImage(String imageName, String trivyVersion = DEFAULT_TRIVY_VERSION, String additionalFlags = "", String severityLevel = TrivySeverityLevel.CRITICAL, String strategy = TrivyScanStrategy.FAIL) {
        int exitCode
        docker.image("aquasec/trivy:${trivyVersion}")
            .mountJenkinsUser()
            .mountDockerSocket()
            .inside("-v ${script.env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
                // Write result to $trivyReportFilename in json format (--format json), which can be converted in the saveFormattedTrivyReport function
                // Exit with exit code 1 if vulnerabilities are found
                exitCode = sh(script: "trivy image --exit-code 1 --format json -o ${trivyReportFilename} --severity ${severityLevel} ${additionalFlags} ${imageName}", returnStatus: true)
            }
        switch (exitCode) {
            case 0:
                // Everything all right, no vulnerabilities
                return true
            case 1:
                // Found vulnerabilities
                // TODO: Set build status according to strategy
                return false
            default:
                throw new TrivyScanException("Error during trivy scan; exit code: " + exitCode)
        }
        // TODO: Include .trivyignore file, if existent. Do not fail if .trivyignore file does not exist.
    }

   /**
    * Save the Trivy scan results as a file with a specific format
    *
    * @param format The format of the output file (@see TrivyScanFormat)
    */
    void saveFormattedTrivyReport(String format = TrivyScanFormat.HTML) {
        // TODO: DO NOT scan again! Take the trivyReportFile and convert its content
        // See https://aquasecurity.github.io/trivy/v0.52/docs/references/configuration/cli/trivy_convert/
    }

}
