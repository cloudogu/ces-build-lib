package com.cloudogu.ces.cesbuildlib

import com.cloudbees.groovy.cps.NonCPS

class Trivy implements Serializable {
    static final String DEFAULT_TRIVY_VERSION = "0.57.1"
    static final String DEFAULT_TRIVY_IMAGE = "aquasec/trivy"
    private script
    private Docker docker
    private String trivyVersion
    private String trivyImage
    private String trivyDirectory = "trivy"

    Trivy(script, String trivyVersion = DEFAULT_TRIVY_VERSION, String trivyImage = DEFAULT_TRIVY_IMAGE, Docker docker = new Docker(script)) {
        this.script = script
        this.trivyVersion = trivyVersion
        this.trivyImage = trivyImage
        this.docker = docker
    }

    /**
     * Scans an image for vulnerabilities.
     * Notes:
     * - Use a .trivyignore file for allowed CVEs
     * - This function will generate a JSON formatted report file which can be converted to other formats via saveFormattedTrivyReport()
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
        Integer exitCode = docker.image("${trivyImage}:${trivyVersion}")
            .mountJenkinsUser()
            .mountDockerSocket()
            .inside("-v ${script.env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
                // Write result to $trivyReportFile in json format (--format json), which can be converted in the saveFormattedTrivyReport function
                // Exit with exit code 10 if vulnerabilities are found or OS is so old that Trivy has no records for it anymore
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
        return scanImage(image + ":" + version, severityLevel, strategy, additionalFlags, trivyReportFile)
    }

    /**
     * Save the Trivy scan results as a file with a specific format
     *
     * @param format The format of the output file {@link TrivyScanFormat}.
     *               You may enter supported formats (sarif, cyclonedx, spdx, spdx-json, github, cosign-vuln, table or json)
     *               or your own template ("template --template @FILENAME").
     *               If you want to convert to a format that requires a list of packages, such as SBOM, you need to add
     *               the `--list-all-pkgs` flag to the {@link Trivy#scanImage} call, when outputting in JSON
     *               (See <a href="https://trivy.dev/latest/docs/configuration/reporting/?ref=anaisurl.com#converting">trivy docs</a>).
     * @param severity Severities of security issues to be added (taken from UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL)
     * @param formattedTrivyReportFilename The file name your report files should get, with file extension. E.g. "ubuntu24report.html"
     * @param trivyReportFile The "trivyReportFile" parameter you used in the "scanImage" function, if it was set
     */
    @NonCPS
    void saveFormattedTrivyReport(String format = TrivyScanFormat.HTML,
                                  String severity = "UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL",
                                  String formattedTrivyReportFilename = null,
                                  String trivyReportFile = "trivy/trivyReport.json") {

        // set default report filename depending on the chosen format
        if (formattedTrivyReportFilename == null) {
            formattedTrivyReportFilename = "formattedTrivyReport" + getFileExtension(format)
        }

        String formatString
        switch (format) {
        // TrivyScanFormat.JSON and TrivyScanFormat.TABLE are handled by the default case, too
            case TrivyScanFormat.HTML:
                formatString = "template --template \"@/contrib/html.tpl\""
                break
            default:
                // You may enter supported formats (sarif, cyclonedx, spdx, spdx-json, github, cosign-vuln, table or json)
                // or your own template ("template --template @FILENAME")
                List<String> trivyFormats = ['sarif', 'cyclonedx', 'spdx', 'spdx-json', 'github', 'cosign-vuln', 'table', 'json']
                // Check if "format" is a custom template from a file
                boolean isTemplateFormat = format ==~ /^template --template @\S+$/
                // Check if "format" is one of the trivyFormats or a template
                if (trivyFormats.any { (format == it) } || isTemplateFormat) {
                    formatString = format
                    break
                } else {
                    script.error("This format did not match the supported formats: " + format)
                    return
                }
        }
        // Validate severity input parameter to prevent injection of additional parameters
        if (!severity.split(',').every { it.trim() in ["UNKNOWN", "LOW", "MEDIUM", "HIGH", "CRITICAL"] }) {
            script.error("The severity levels provided ($severity) do not match the applicable levels (UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL).")
        }

        docker.image("${trivyImage}:${trivyVersion}")
            .inside("-v ${script.env.WORKSPACE}/.trivy/.cache:/root/.cache/") {
                script.sh(script: "trivy convert --format ${formatString} --severity ${severity} --output ${trivyDirectory}/${formattedTrivyReportFilename} ${trivyReportFile}")
            }
        script.archiveArtifacts artifacts: "${trivyDirectory}/${formattedTrivyReportFilename}.*", allowEmptyArchive: true
    }

    private static String getFileExtension(String format) {
        return TrivyScanFormat.isStandardScanFormat(format) ? "." + format : ".txt"
    }
}
