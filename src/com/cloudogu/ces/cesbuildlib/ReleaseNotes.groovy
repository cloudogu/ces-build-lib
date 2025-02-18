package com.cloudogu.ces.cesbuildlib

/**
 * Enables interaction with release notes files
 */
class ReleaseNotes implements Serializable {
    private script
    private String releaseNotesFileNameDE
    private String releaseNotesFileNameEN

    ReleaseNotes(script) {
        this(script, 'docs/gui/release_notes_de.md', 'docs/gui/release_notes_en.md')
    }

    ReleaseNotes(script, releaseNotesFileNameDE, releaseNotesFileNameEN) {
        this.script = script
        this.releaseNotesFileNameDE = releaseNotesFileNameDE
        this.releaseNotesFileNameEN = releaseNotesFileNameEN
    }

    /**
     * @return Returns the content of german release notes file.
     */
    private String readReleaseNotesDE(){
        script.readFile releaseNotesFileNameDE
    }

    /**
     * @return Returns the content of english release notes file.
     */
    private String readReleaseNotesEN(){
        script.readFile releaseNotesFileNameEN
    }

    /**
     * Extracts the changes for a given version out of the german release notes.
     *
     * @param releaseVersion The version to get the changes for.
     * @return Returns the changes as String.
     */
    String changesForDEVersion(String releaseVersion) {
        def releaseNotesDE = readReleaseNotesDE()
        def start = changesStartIndex(releaseNotesDE, releaseVersion)
        def end = changesEndIndex(releaseNotesDE, start)
        return escapeForJson(releaseNotesDE.substring(start, end).trim())
    }

    /**
     * Extracts the changes for a given version out of the english release notes.
     *
     * @param releaseVersion The version to get the changes for.
     * @return Returns the changes as String.
     */
    String changesForENVersion(String releaseVersion) {
        def releaseNotesEN = readReleaseNotesEN()
        def start = changesStartIndex(releaseNotesEN, releaseVersion)
        def end = changesEndIndex(releaseNotesEN, start)
        return escapeForJson(releaseNotesEN.substring(start, end).trim())
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
     * Returns the start index of changes of a specific release version in the release notes.
     *
     * @param releaseVersion The version to get the changes for.
     * @return Returns the index in the release notes string where the changes start.
     */
    private static int changesStartIndex(String releaseNotes, String releaseVersion) {
        def index = releaseNotes.indexOf("## [${releaseVersion}]")
        if (index == -1){
            throw new IllegalArgumentException("The desired version '${releaseVersion}' could not be found in the release notes.")
        }
        def offset = releaseNotes.substring(index).indexOf("\n")
        return index + offset
    }

    /**
     * Returns the end index of changes of a specific release version in the release notes.
     *
     * @param start The start index of the changes for this version.
     * @return Returns the index in the release notes string where the changes end.
     */
    private static int changesEndIndex(String releaseNotes, int start) {
        def releaseNotesAfterStartIndex = releaseNotes.substring(start)
        def index = releaseNotesAfterStartIndex.indexOf("\n## [")
        if (index == -1) {
            return releaseNotes.length()
        }
        return index + start
    }
}
