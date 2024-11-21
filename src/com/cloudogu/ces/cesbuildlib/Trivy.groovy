package com.cloudogu.ces.cesbuildlib

class Trivy implements Serializable {
    static final String DEFAULT_TRIVY_VERSION = "0.57.1"
    private script
    private Docker docker
    private String trivyVersion
    private String trivyDirectory = ".trivy"
    private String trivyReportFilenameWithoutExtension = trivyDirectory+"/trivyReport"

    Trivy(script, Docker docker = new Docker(script), String trivyVersion = "0.57.1") {
        this.script = script
        this.docker = docker
        this.trivyVersion = trivyVersion
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
    boolean scanImage(
        String imageName,
        String trivyReportFilename = "${this.script.env.WORKSPACE}/.trivy/trivyReport.json",
        String additionalFlags = "",
        String severityLevel = TrivySeverityLevel.CRITICAL,
        String strategy = TrivyScanStrategy.FAIL
    ) {
        int exitCode
        docker.image("aquasec/trivy:${trivyVersion}")
            .mountJenkinsUser()
            .mountDockerSocket()
            .inside("-v ${script.env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
                // Write result to $trivyReportFilename in json format (--format json), which can be converted in the saveFormattedTrivyReport function
                // Exit with exit code 1 if vulnerabilities are found
                script.sh("mkdir -p " + trivyDirectory)
                exitCode = script.sh(script: "trivy image --exit-code 10 --exit-on-eol 10 --format ${TrivyScanFormat.JSON} -o ${trivyReportFilename} --severity ${severityLevel} ${additionalFlags} ${imageName}", returnStatus: true)
            }
        switch (exitCode) {
            case 0:
                // Everything all right, no vulnerabilities
                return true
            case 10:
                // Found vulnerabilities
                // TODO: Set build status according to strategy
                return false
            default:
                script.error("Error during trivy scan; exit code: " + exitCode)
        }
        // TODO: Include .trivyignore file, if existent. Do not fail if .trivyignore file does not exist.
    }

    /**
     * Save the Trivy scan results as a file with a specific format
     *
     * @param format The format of the output file (@see TrivyScanFormat)
     */
    void saveFormattedTrivyReport(String format = TrivyScanFormat.HTML, String trivyReportFilename = "${script.env.WORKSPACE}/.trivy/trivyReport.json") {
        String formatExtension
        switch (format) {
            case TrivyScanFormat.HTML:
                formatExtension = "html"
                // TODO: html is no standard convert format. Use a template!
            case TrivyScanFormat.JSON:
                // Result file is already in JSON format
                return
            case TrivyScanFormat.TABLE:
                formatExtension = "table"
            default:
                // TODO: Do nothing? Throw exception? idk
                break
        }
        docker.image("aquasec/trivy:${trivyVersion}")
            .inside("-v ${script.env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
                script.sh(script: "trivy convert --format ${format} --output ${trivyReportFilenameWithoutExtension}.${formatExtension} ${trivyReportFilename}")
            }

        script.archiveArtifacts artifacts: "${trivyReportFilenameWithoutExtension}.${format}", allowEmptyArchive: true
    }
}
