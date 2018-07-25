package com.cloudogu.ces.cesbuildlib

/**
 * Abstraction for SonarCloud. More or less a special SonarQube instance.
 *
 * The integration into GitHub, BitBucket and such is done via 3rd party integrations into those tools.
 * Normal SonarQube uses e.g. the GitHub plugin.
 */
class SonarCloud extends SonarQube {

    SonarCloud(script, Map config) {
        super(script, config)

        this.isUsingBranchPlugin = true
    }

    @Override
    boolean doWaitForPullRequestQualityGateWebhookToBeCalled() {
        // PRs are now also analyzed on the SonarCloud server, no more preview. Analyze normally.
        return doWaitForQualityGateWebhookToBeCalled()
    }

    @Override
    void initMavenForPullRequest(Maven mvn) {
        script.echo "SonarQube analyzing PullRequest ${script.env.CHANGE_ID}. Using preview mode. "

        // See https://sonarcloud.io/documentation/integrations/github
        mvn.additionalArgs +=
                "-Dsonar.pullrequest.base=${script.env.CHANGE_TARGET} " +
                "-Dsonar.pullrequest.branch=${script.env.CHANGE_BRANCH} " +
                "-Dsonar.pullrequest.key=${script.env.CHANGE_ID} " +
                "-Dsonar.pullrequest.provider=GitHub " +
                "-Dsonar.pullrequest.github.repository=${new Git(script).gitHubRepositoryName} "
    }

    @Override
    protected void initMaven(Maven mvn) {
        super.initMaven(mvn)

        if (config['sonarOrganization']) {
            mvn.additionalArgs += " -Dsonar.organization=${config['sonarOrganization']} "
        }
    }

    @Override
    protected void validateMandatoryFieldsWithoutSonarQubeEnv() {
        super.validateMandatoryFieldsWithoutSonarQubeEnv()
        // When using SonarQube env, the sonarOrganization might be set there as SONAR_EXTRA_PROPS
        validateFieldPresent(config, 'sonarOrganization')

    }
}
