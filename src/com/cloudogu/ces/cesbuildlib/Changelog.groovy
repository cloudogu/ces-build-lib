package com.cloudogu.ces.cesbuildlib

/**
 * Provides the functionality to read changes of a specific version in a changelog that is
 * based on the changelog format on https://keepachangelog.com/.
 */
class Changelog implements Serializable {
    private script
    private String changelog
    Sh sh

    Changelog(script) {
        this.script = script
        this.sh = new Sh(script)
        this.changelog = "CHANGELOG.md"
    }

    Changelog(script, changelog) {
        this.script = script
        this.sh = new Sh(script)
        this.changelog = changelog
    }

    /**
     * @return Returns the content of the given changelog.
     */
    private String getChangelog(){
        this.sh.returnStdOut("cat ${changelog}")
    }

    /**
     * Extracts the changes for a given version out of the changelog.
     *
     * @param releaseVersion The version to get the changes for.
     * @return Returns the changes as String.
     */
    String getChangesForVersion(String releaseVersion) {
        def start = getChangelogStartIndex(releaseVersion)
        if (start == -1){
            throw new Exception("The desired version '${releaseVersion}' could not be found in the changelog.")
        }

        def end = getChangelogEndIndex(start)
        return formatForJson(getChangelog().substring(start, end).trim())
    }

    /**
     * Removes characters from a string that could break the json struct when passing the string as json value.
     *
     * @param string The string to format.
     * @return Returns the formatted string.
     */
    String formatForJson(String string) {
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
    private int getChangelogStartIndex(String releaseVersion) {
        int offset = Math.min("## [${releaseVersion}]".length(), 0)
        return getChangelog().indexOf("## [${releaseVersion}]") + offset
    }

    /**
     * Returns the end index of changes of a specific release version in the changelog.
     *
     * @param start The start index of the changes for this version.
     * @return Returns the index in the changelog string where the changes end.
     */
    private int getChangelogEndIndex(int start) {
        def changelogAfterStartIndex = getChangelog().substring(start)
        def index = changelogAfterStartIndex.indexOf("\n## [")
        if (index == -1) {
            return getChangelog().length()
        }
        return index + start
    }
}
