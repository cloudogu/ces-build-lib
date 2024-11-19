package com.cloudogu.ces.cesbuildlib

/**
 * Defines aggregated vulnerability levels
 */
class TrivySeverityLevel {
    /**
     * Only critical vulnerabilities.
     */
    static String CRITICAL = "CRITICAL"

    /**
     * High or critical vulnerabilities.
     */
    static String HIGH = "CRITICAL,HIGH"

    /**
     * Medium or higher vulnerabilities.
     */
    static String MEDIUM = "CRITICAL,HIGH,MEDIUM"

    /**
     * All vulnerabilities.
     */
    static String ALL = "UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL"
}
