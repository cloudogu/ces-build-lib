package com.cloudogu.ces.cesbuildlib

import hudson.model.Cause
import hudson.triggers.TimerTrigger
import org.jenkinsci.plugins.workflow.cps.replay.ReplayCause
import org.junit.Test

import static junit.framework.TestCase.assertFalse

class BuildTest {

    ScriptMock scriptMock = new ScriptMock()

    @Test
    void isTimeTriggeredBuild() {
        // hudson.model.Cause$UserIdCause@11725044, org.jenkinsci.plugins.workflow.cps.replay.ReplayCause@7147f8fe
        scriptMock.currentBuild.rawBuild.causes = [new Cause.UserIdCause(), new ReplayCause(),
                                                   new TimerTrigger.TimerTriggerCause()]
        Build build = new Build(scriptMock)

        assertFalse(build.isTimeTriggeredBuild())
    }

    @Test
    void isNotTimeTriggeredBuild() {
        // hudson.model.Cause$UserIdCause@11725044, org.jenkinsci.plugins.workflow.cps.replay.ReplayCause@7147f8fe
        scriptMock.currentBuild.rawBuild.causes = [new Cause.UserIdCause(), new ReplayCause()]
        Build build = new Build(scriptMock)

        assertFalse(build.isTimeTriggeredBuild())
    }

    private static class ScriptMock {
        def currentBuild = new Object() {
            Object rawBuild = new Object() {
                def causes
            }
        }
    }
}
