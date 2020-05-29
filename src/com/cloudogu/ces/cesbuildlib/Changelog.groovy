package com.cloudogu.ces.cesbuildlib

/**
 * Provides the functionality to read changes of a specific version in a changelog that is
 * based on the changelog format on https://keepachangelog.com/.
 */
class Changelog implements Serializable {
    private script
    private String changelogFileName

    Changelog(script) {
        this(script, 'CHANGELOG.md')
    }

    Changelog(script, changelogFileName) {
        this.script = script
        this.changelogFileName = changelogFileName
    }

    /**
     * @return Returns the content of the given changelog.
     */
    private String readChangelog(){
        script.readFile changelogFileName
    }

    /**
     * Extracts the changes for a given version out of the changelog.
     *
     * @param releaseVersion The version to get the changes for.
     * @return Returns the changes as String.
     */
    String changesForVersion(String releaseVersion) {
        def changelog = readChangelog()
        def start = changesStartIndex(changelog, releaseVersion)
        def end = changesEndIndex(changelog, start)
        return escapeForJson(changelog.substring(start, end).trim())
    }

    /**
     * Removes characters from a string that could break the json struct when passing the string as json value.
     *
     * @param string The string to format.
     * @return Returns the formatted string.
     */
    private static String escapeForJson(String string) {
        return string
                .replace("\"", "")
                .replace("'", "")
                .replace("\\", "")
                .replace("\n", "\\n")
    }

    /**
     * Returns the start index of changes of a specific release version in the changelog.
     *
     * @param releaseVersion The version to get the changes for.
     * @return Returns the index in the changelog string where the changes start.
     */
    private static int changesStartIndex(String changelog, String releaseVersion) {
        def index = changelog.indexOf("## [${releaseVersion}]")
        if (index == -1){
            throw new IllegalArgumentException("The desired version '${releaseVersion}' could not be found in the changelog.")
        }
        def offset = changelog.substring(index).indexOf("\n")
        return index + offset
    }

    /**
     * Returns the end index of changes of a specific release version in the changelog.
     *
     * @param start The start index of the changes for this version.
     * @return Returns the index in the changelog string where the changes end.
     */
    private static int changesEndIndex(String changelog, int start) {
        def changelogAfterStartIndex = changelog.substring(start)
        def index = changelogAfterStartIndex.indexOf("\n## [")
        if (index == -1) {
            return changelog.length()
        }
        return index + start
    }
}
