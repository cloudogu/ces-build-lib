package com.cloudogu.ces.cesbuildlib

/**
 * Defines the output format for the trivy report.
 */
class TrivyScanFormat {
    /**
     * Output as HTML file.
     */
    static String HTML = "html"

    /**
     * Output as JSON file.
     */
    static String JSON = "json"

    /**
     * Output as table.
     */
    static String TABLE = "table"

    static boolean isStandardScanFormat(String format) {
        return format == HTML || format == JSON || format == TABLE
    }
}
