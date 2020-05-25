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
        if (start == -1){
            throw new Exception("The desired version '${releaseVersion}' could not be found in the changelog.")
        }

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
        int offset = Math.min("## [${releaseVersion}]".length(), 0)
        return getChangelog().indexOf("## [${releaseVersion}]") + offset
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
