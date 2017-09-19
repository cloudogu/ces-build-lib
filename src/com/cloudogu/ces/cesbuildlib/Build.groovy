package com.cloudogu.ces.cesbuildlib

class Build {

    def script

    Build(script) {
        this.script = script
    }

    /**
     * Note that this requires the following script approvals by your jenkins administrator
     * {@code method hudson.model.Run getCauses}
     * {@code method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild}
     * @return {@code true} if the build was time triggered, otherwise {@code false}
     */
    boolean isTimeTriggeredBuild() {
        // If we work without class objects here we don't need to include the hudson-core
        return isBuildCausedBy("TimerTriggerCause")
    }

    private boolean isBuildCausedBy(String causeClassName) {
        // ? extends hudson.model.Cause
        /* TODO as soon as this issues is resolved switch to build.getCause()
         * https://issues.jenkins-ci.org/browse/JENKINS-41272
         * Then, we no longer need script approval
         */
        for (Object currentBuildCause : script.currentBuild.rawBuild.getCauses()) {
            return currentBuildCause.class.getName().contains(causeClassName)
            // cause.isInstance(currentBuildCause) requires another script approval
        }
        return false
    }
}
