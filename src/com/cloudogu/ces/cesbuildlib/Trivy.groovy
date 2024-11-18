package com.cloudogu.ces.cesbuildlib

class Trivy implements Serializable {
    private script
    private String trivyReportFilename

    Trivy(script, String trivyReportFilename = "${script.env.WORKSPACE}/.trivy/trivyReport.json") {
        this.script = script
        this.trivyReportFilename = trivyReportFilename
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
     * @param scanLevel The vulnerability level to scan. Can be a member of TrivyScanLevel or a custom String (e.g. 'CRITICAL,LOW')
     * @param strategy The strategy to follow after the scan. Should the build become unstable or failed? Or Should any vulnerability be ignored? (@see TrivyScanStrategy)
     * // TODO: A strategy could be implemented by the user via the exit codes of this function. Should we remove the strategy parameter?
     * @return Returns 0 if the scan was ok (no vulnerability found); returns 1 if any vulnerability was found
     */
    int scanImage(String imageName, String trivyVersion = "0.57.0", String additionalFlags = "", String scanLevel = TrivyScanLevel.CRITICAL, String strategy = TrivyScanStrategy.FAIL) {
        int exitCode = 255
        // TODO: Run trivy scan inside Docker container, e.g. via Jenkins' Docker.image() function
        // See runTrivyInDocker function: https://github.com/cloudogu/ces-build-lib/blob/c48273409f8f506e31872fe2857650bbfc76a222/vars/findVulnerabilitiesWithTrivy.groovy#L48
        // TODO: Write result to trivyReportFile in json format (--format json), which can be converted in the saveFormattedTrivyReport function
        // TODO: Include .trivyignore file, if existent. Do not fail if .trivyignore file does not exist.
        return exitCode
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
