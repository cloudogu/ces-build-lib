package com.cloudogu.ces.cesbuildlib


boolean call() {
    currentBuild.currentResult == 'SUCCESS' &&
        // Build result == SUCCESS seems not to set be during pipeline execution.
        (currentBuild.result == null || currentBuild.result == 'SUCCESS')
}
