package com.cloudogu.ces.cesbuildlib

class Trivy implements Serializable {
    static final String DEFAULT_TRIVY_VERSION = "0.57.1"
    private script
    private Docker docker
    private String trivyVersion
    private String trivyDirectory = "trivy"

    Trivy(script, String trivyVersion = "0.57.1", Docker docker = new Docker(script)) {
        this.script = script
        this.trivyVersion = trivyVersion
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
     * @param severityLevel The vulnerability level to scan. Can be a member of TrivySeverityLevel or a custom String (e.g. 'CRITICAL,LOW')
     * @param strategy The strategy to follow after the scan. Should the build become unstable or failed? Or Should any vulnerability be ignored? (@see TrivyScanStrategy)
     * @param additionalFlags Additional Trivy command flags
     * @param trivyReportFile Location of Trivy report file. Should be set individually when scanning multiple images in the same pipeline
     * @return Returns true if the scan was ok (no vulnerability found); returns false if any vulnerability was found
     */
    boolean scanImage(
        String imageName,
        String severityLevel = TrivySeverityLevel.CRITICAL,
        String strategy = TrivyScanStrategy.UNSTABLE,
        // Avoid rate limits of default Trivy database source
        String additionalFlags = "--db-repository public.ecr.aws/aquasecurity/trivy-db --java-db-repository public.ecr.aws/aquasecurity/trivy-java-db",
        String trivyReportFile = "trivy/trivyReport.json"
    ) {
        Integer exitCode = docker.image("aquasec/trivy:${trivyVersion}")
            .mountJenkinsUser()
            .mountDockerSocket()
            .inside("-v ${script.env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
                // Write result to $trivyReportFile in json format (--format json), which can be converted in the saveFormattedTrivyReport function
                // Exit with exit code 1 if vulnerabilities are found
                script.sh("mkdir -p " + trivyDirectory)
                script.sh(script: "trivy image --exit-code 10 --exit-on-eol 10 --format ${TrivyScanFormat.JSON} -o ${trivyReportFile} --severity ${severityLevel} ${additionalFlags} ${imageName}", returnStatus: true)
            }
        switch (exitCode) {
            case 0:
                // Everything all right, no vulnerabilities
                return true
            case 10:
                // Found vulnerabilities
                // Set build status according to strategy
                switch (strategy) {
                    case TrivyScanStrategy.IGNORE:
                        break
                    case TrivyScanStrategy.UNSTABLE:
                        script.archiveArtifacts artifacts: "${trivyReportFile}", allowEmptyArchive: true
                        script.unstable("Trivy has found vulnerabilities in image " + imageName + ". See " + trivyReportFile)
                        break
                    case TrivyScanStrategy.FAIL:
                        script.archiveArtifacts artifacts: "${trivyReportFile}", allowEmptyArchive: true
                        script.error("Trivy has found vulnerabilities in image " + imageName + ". See " + trivyReportFile)
                        break
                }
                return false
            default:
                script.error("Error during trivy scan; exit code: " + exitCode)
        }
    }

    /**
     * Scans a dogu image for vulnerabilities.
     * Notes:
     * - Use a .trivyignore file for allowed CVEs
     * - This function will generate a JSON formatted report file which can be converted to other formats via saveFormattedTrivyReport()
     * - Evaluate via exit codes: 0 = no vulnerability; 1 = vulnerabilities found; other = function call failed
     *
     * @param doguDir The directory the dogu code (dogu.json) is located
     * @param severityLevel The vulnerability level to scan. Can be a member of TrivySeverityLevel or a custom String (e.g. 'CRITICAL,LOW')
     * @param strategy The strategy to follow after the scan. Should the build become unstable or failed? Or Should any vulnerability be ignored? (@see TrivyScanStrategy)
     * @param additionalFlags Additional Trivy command flags
     * @param trivyReportFile Location of Trivy report file. Should be set individually when scanning multiple images in the same pipeline
     * @return Returns true if the scan was ok (no vulnerability found); returns false if any vulnerability was found
     */
    boolean scanDogu(
        String doguDir = ".",
        String severityLevel = TrivySeverityLevel.CRITICAL,
        String strategy = TrivyScanStrategy.UNSTABLE,
        // Avoid rate limits of default Trivy database source
        String additionalFlags = "--db-repository public.ecr.aws/aquasecurity/trivy-db --java-db-repository public.ecr.aws/aquasecurity/trivy-java-db",
        String trivyReportFile = "trivy/trivyReport.json"
    ) {
        String image = script.sh(script: "jq .Image ${doguDir}/dogu.json", returnStdout: true).trim()
        String version = script.sh(script: "jq .Version ${doguDir}/dogu.json", returnStdout: true).trim()
        return scanImage(image+":"+version, severityLevel, strategy, additionalFlags, trivyReportFile)
    }

    /**
     * Save the Trivy scan results as a file with a specific format
     *
     * @param format The format of the output file (@see TrivyScanFormat)
     * @param formattedTrivyReportFilename The file name your report files should get, without file extension. E.g. "ubuntu24report"
     * @param trivyReportFile The "trivyReportFile" parameter you used in the "scanImage" function, if it was set
     */
    void saveFormattedTrivyReport(String format = TrivyScanFormat.HTML, String formattedTrivyReportFilename = "trivyReport", String trivyReportFile = "trivy/trivyReport.json") {
        String fileExtension
        String formatString
        String trivyDirectory = "trivy/"
        switch (format) {
            case TrivyScanFormat.HTML:
                formatString = "template --template \"@/contrib/html.tpl\""
                fileExtension = "html"
                break
            case TrivyScanFormat.JSON:
                formatString = "json"
                fileExtension = "json"
                break
            case TrivyScanFormat.TABLE:
                formatString = "table"
                fileExtension = "txt"
                break
            default:
                script.error("This format did not match the supported formats: " + format)
                return
        }
        docker.image("aquasec/trivy:${trivyVersion}")
            .inside("-v ${script.env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
                script.sh(script: "trivy convert --format ${formatString} --output ${trivyDirectory}${formattedTrivyReportFilename}.${fileExtension} ${trivyReportFile}")
            }
        script.archiveArtifacts artifacts: "${trivyDirectory}${formattedTrivyReportFilename}.*", allowEmptyArchive: true
    }
}
