package com.cloudogu.ces.cesbuildlib

/**
 * Defines aggregated vulnerability levels
 */
class TrivyScanLevel {
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
     * All vunlerabilities.
     */
    static String ALL = "UNKNOWN,LOW,MEDIUM,HIGH,CRITICAL"
}
