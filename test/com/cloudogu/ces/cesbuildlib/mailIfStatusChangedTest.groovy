package com.cloudogu.ces.cesbuildlib

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

class MailIfStatusChangedTest extends BasePipelineTest {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void mailIfStatusChangedTest() {
        def stepParams = [:]
        helper.registerAllowedMethod("step", [Map.class], {paramMap -> stepParams = paramMap})
        binding.getVariable('currentBuild').currentResult = 'Not SUCCESS'
        def expectedResult = 'Do not change this'
        binding.getVariable('currentBuild').result = expectedResult

        def script = loadScript('vars/mailIfStatusChanged.groovy')
        script.call('a@b.cd')
        assert stepParams.recipients == 'a@b.cd'
        assert stepParams.$class == 'Mailer'
        assert binding.getVariable('currentBuild').result == expectedResult
    }

    @Test
    void mailIfStatusChangedIfStatusIsBackToNormal() {
        def stepParams = [:]
        helper.registerAllowedMethod("step", [Map.class], {paramMap -> stepParams = paramMap})
        binding.getVariable('currentBuild').result = null
        binding.getVariable('currentBuild').currentResult = 'SUCCESS'

        def script = loadScript('vars/mailIfStatusChanged.groovy')
        script.call('a@b.cd')
        assert stepParams.recipients == 'a@b.cd'
        assert stepParams.$class == 'Mailer'
        assert binding.getVariable('currentBuild').result == 'SUCCESS'
    }
}