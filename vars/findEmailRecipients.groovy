package com.cloudogu.ces.cesbuildlib

def call(String defaultRecipients) {
    def isStableBranch = env.BRANCH_NAME in ['master', 'develop']
    String commitAuthorEmail = new Git(this).commitAuthorEmail

    if (commitAuthorEmail.isEmpty()) {
        return defaultRecipients
    }

    if (isStableBranch) {

        if (!defaultRecipients.contains(commitAuthorEmail)) {
            defaultRecipients += ",$commitAuthorEmail"
        }

        return defaultRecipients

    } else {
        return commitAuthorEmail
    }
}