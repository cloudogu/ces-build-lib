package com.cloudogu.ces.cesbuildlib

class ChangelogParser implements Serializable {
    private script
    private Changelog changelog
    Sh sh

    ChangelogParser(script) {
        this.script = script
        this.sh = new Sh(script)
        this.changelog = new Changelog("CHANGELOG.md", script)
    }

    ChangelogParser(script, changelog) {
        this.script = script
        this.sh = new Sh(script)
        this.changelog = changelog
    }

    String getChangesForVersion(String releaseVersion) {
        def start = getChangelogStartIndex(releaseVersion)
        def end = getChangelogEndIndex(start)
        def changelog = this.changelog.get()
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
        def changelog = this.changelog.get()
        return changelog.indexOf("## [${releaseVersion}]") + "## [${releaseVersion}]".length()
    }

    private int getChangelogEndIndex(int start) {
        def changelog = this.changelog.get().substring(start)
        def index = changelog.indexOf("\n## [")
        if (index == -1) {
            return this.changelog.get().length()
        }
        return index + start
    }
}
