package com.cloudogu.ces.cesbuildlib

def call(ReleaseNotes releaseNotes = new ReleaseNotes(this)) {
    // Checking if this is associated with a pull request
    if (env.CHANGE_TARGET) {
        echo "Checking release notes..."
        String newChangesReleaseNotesDE = releaseNotes.changesForDEVersion("Unreleased")
        if (!newChangesReleaseNotesDE || newChangesReleaseNotesDE.allWhitespace) {
            unstable('Release Notes should contain new change entries in the `[Unreleased]` section but none were found in the german version')
        }
        String newChangesReleaseNotesEN = releaseNotes.changesForENVersion("Unreleased")
        if (!newChangesReleaseNotesEN || newChangesReleaseNotesEN.allWhitespace) {
            unstable('Release Notes should contain new change entries in the `[Unreleased]` section but none were found in the english version')
        }
    }
}
