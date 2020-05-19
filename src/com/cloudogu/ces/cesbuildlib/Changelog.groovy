package com.cloudogu.ces.cesbuildlib

class Changelog implements Serializable {
    private script
    private String changelog
    Sh sh

    Changelog(script) {
        this.script = script
        this.sh = new Sh(script)
        this.changelog = this.sh.returnStdOut("cat CHANGELOG.md")
    }

    Changelog(script, changelog) {
        this.script = script
        this.sh = new Sh(script)
        this.changelog = this.sh.returnStdOut("cat ${changelog}")
    }

    String getChangesForVersion(String releaseVersion) {
        def start = getChangelogStartIndex(releaseVersion)
        def end = getChangelogEndIndex(start)
        return formatForJson(changelog.substring(start, end).trim())
    }

    String formatForJson(String string) {
        return string
                .replace("\"", "")
                .replace("'", "")
                .replace("\\", "")
                .replace("\n", "\\n")
    }

    private int getChangelogStartIndex(String releaseVersion) {
        return changelog.indexOf("## [${releaseVersion}]") + "## [${releaseVersion}]".length()
    }

    private int getChangelogEndIndex(int start) {
        def changelogAfterStartIndex = changelog.substring(start)
        def index = changelogAfterStartIndex.indexOf("\n## [")
        if (index == -1) {
            return changelog.length()
        }
        return index + start
    }
}
