package com.cloudogu.ces.cesbuildlib


def call(String recipients) {
    
    // Also send "back to normal" emails and . Mailer seems to check build result, but SUCCESS is not set during pipeline execution.
    if (!currentBuild.result) {
        currentBuild.result = currentBuild.currentResult
    }
    
    step([$class: 'Mailer', recipients: recipients, 
          sendToIndividuals: true,
          // Necessary for "still unstable" emails
          notifyEveryUnstableBuild: true])
}
