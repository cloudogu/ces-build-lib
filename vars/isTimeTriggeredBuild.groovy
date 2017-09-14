package com.cloudogu.ces.cesbuildlib

def call() {
    return isBuildCausedBy(hudson.triggers.TimerTrigger.TimerTriggerCause.class)
}

private boolean isBuildCausedBy(Class<? extends hudson.model.Cause> cause) {
    for (Object currentBuildCause : currentBuild.rawBuild.getCauses()) {
        echo("current build cause $currentBuildCause")
        if (cause.isInstance(currentBuildCause)) {
            return true
        }
    }
    return false
}
