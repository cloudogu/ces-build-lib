package com.cloudogu.ces.cesbuildlib

class Changelog implements Serializable {
    private script
    Sh sh

    Changelog(script) {
        this.script = script
        this.sh = new Sh(script)
    }

    String getChangelog(String releaseVersion) {
        def start = getChangelogStartIndex(releaseVersion)
        def end = getChangelogEndIndex(start)
        def output = sh.returnStdOut("sed '${start},${end}!d' CHANGELOG.md")
        return output.replace("\"", "").replace("'", "").replace("\\", "").replace("\n", "\\n")
    }

    private int getChangelogStartIndex(String releaseVersion) {
        def startLineString = "## \\[${releaseVersion}\\]"
        def output = sh.returnStdOut("grep -n \"${startLineString}\" CHANGELOG.md | head -1 | sed s/\"^\\([0-9]*\\)[:].*\$\"/\"\\1\"/g")
        return (output as int) + 1
    }

    private String getChangelogEndIndex(int start) {
        def output = sh.returnStdOut("tail -n +${start + 1} CHANGELOG.md |grep -n \"^## \\[.*\\]\" | sed s/\"^\\([0-9]*\\)[:].*\$\"/\"\\1\"/g | head -1")
        if ((output as String).length() > 0) {
            return ((output as int) + start - 1) as String
        }
        return "\$"
    }
}
