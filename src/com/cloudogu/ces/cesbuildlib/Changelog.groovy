package com.cloudogu.ces.cesbuildlib

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

    private String getChangelog(){
        this.sh.returnStdOut("cat ${changelog}")
    }

    String getChangesForVersion(String releaseVersion) {
        def start = getChangelogStartIndex(releaseVersion)
        def end = getChangelogEndIndex(start)
        return formatForJson(getChangelog().substring(start, end).trim())
    }

    String formatForJson(String string) {
        return string
                .replace("\"", "")
                .replace("'", "")
                .replace("\\", "")
                .replace("\n", "\\n")
    }

    private int getChangelogStartIndex(String releaseVersion) {
        return getChangelog().indexOf("## [${releaseVersion}]") + "## [${releaseVersion}]".length()
    }

    private int getChangelogEndIndex(int start) {
        def changelogAfterStartIndex = getChangelog().substring(start)
        def index = changelogAfterStartIndex.indexOf("\n## [")
        if (index == -1) {
            return getChangelog().length()
        }
        return index + start
    }
}
