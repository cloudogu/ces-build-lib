package com.cloudogu.ces.cesbuildlib

def call(Changelog changelog = new Changelog(this), ReleaseNotes releaseNotes = new ReleaseNotes(this)) {
    // Checking if this is associated with a pull request
    if (env.CHANGE_TARGET) {
        echo "Checking changelog..."
        String newChanges = changelog.changesForVersion('Unreleased')
        if (!newChanges || newChanges.allWhitespace) {
            unstable('CHANGELOG.md should contain new change entries in the `[Unreleased]` section but none were found.')
        }
    }
}
