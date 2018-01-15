package com.cloudogu.ces.cesbuildlib

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

class FindEmailRecipientsTest extends BasePipelineTest {

    String expectedDefaultRecipients = 'a@b.c,d@e.f'
    String expectedCommitAuthor = 'q@q.q'
    def script

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        script = loadScript('vars/findEmailRecipients.groovy')
    }

    @Test
    void developBranch() {
        script.env = [BRANCH_NAME: 'develop']
        setCommitAuthor(expectedCommitAuthor)

        def actualRecipients = script.call(expectedDefaultRecipients)

        assert actualRecipients == "$expectedDefaultRecipients,$expectedCommitAuthor"
    }

    @Test
    void masterBranch() {
        script.env = [BRANCH_NAME: 'master']
        setCommitAuthor(expectedCommitAuthor)

        def actualRecipients = script.call(expectedDefaultRecipients)

        assert actualRecipients == "$expectedDefaultRecipients,$expectedCommitAuthor"
    }

    @Test
    void unstableBranch() {
        script.env = [BRANCH_NAME: 'someBranch']
        setCommitAuthor(expectedCommitAuthor)

        def actualRecipients = script.call(expectedDefaultRecipients)

        assert actualRecipients == expectedCommitAuthor
    }

    @Test
    void stableBranchCommitAuthorContainedInDefaultRecipients() {
        script.env = [BRANCH_NAME: 'master']
        setCommitAuthor('d@e.f')

        def actualRecipients = script.call('a@b.c,d@e.f')

        assert actualRecipients == 'a@b.c,d@e.f'
    }

    @Test
    void commitAuthorEmpty() {
        script.env = [BRANCH_NAME: 'someBranch']
        setCommitAuthor('')

        def actualRecipients = script.call(expectedDefaultRecipients)

        assert actualRecipients == expectedDefaultRecipients
    }

    def setCommitAuthor(String commitAuthor) {
        helper.registerAllowedMethod("sh", [Map.class], { paramMap -> "<$commitAuthor>" })
    }
}