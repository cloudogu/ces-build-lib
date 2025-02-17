package com.cloudogu.ces.cesbuildlib

def call(ReleaseNotes releaseNotes = new ReleaseNotes(this)) {
    // Checking if this is associated with a pull request
    if (env.CHANGE_TARGET) {
        echo "Checking release notes..."
        String newChangesReleaseNotes = releaseNotes.changesForDEVersion("Unreleased")
        if (!newChangesReleaseNotes || newChangesReleaseNotes.allWhitespace) {
            unstable('Release Notes should contain new change entries in the `[Unreleased]` section but none were found.')
        }
    }
}
