package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonOutput

class SCMManager implements Serializable {

    private script
    private HttpClient http
    String repositoryUrl

    SCMManager(script, credentials) {
        this.script = script
        this.http = new HttpClient(script, credentials)
    }

    String searchPullRequestIdByTitle(String title) {
        def pullRequest
        for (Map pr : getPullRequests()) {
            if (pr.title == title) {
                pullRequest = pr
            }
        }

        if (pullRequest) {
            return pullRequest.id.toString()
        } else {
            return ""
        }
    }

    String createPullRequest(String source, String target, String title, String description) {
        def dataJson = JsonOutput.toJson([
            title      : title,
            description: description,
            source     : source,
            target     : target
        ])
        def httpResponse = http.post("https://${this.repositoryUrl}", 'application/vnd.scmm-pullRequest+json;v=2', dataJson)

        script.echo "Creating pull request yields httpCode: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "201") {
            script.echo 'WARNING: Http status code indicates, that pull request was not created'
            script.unstable 'Could not create pull request'
            return ''
        }

        // example: "location: https://some/pr/42" - extract ide
        return httpResponse.headers.location.split("/")[-1]
    }

    void updateDescription(String pullRequestId, String title, String description) {
        // In order to update the description put in also the title. Otherwise the title is overwritten with an empty string.
        def dataJson = JsonOutput.toJson([
            title      : title,
            description: description
        ])

        def httpResponse = http.put("https://${this.repositoryUrl}/${pullRequestId}", 'application/vnd.scmm-pullRequest+json;v=2', dataJson)

        script.echo "Description update yields http_code: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "204") {
            script.unstable 'Could not update description'
        }
    }

    void addComment(String pullRequestId, String comment) {
        def dataJson = JsonOutput.toJson([
            comment: comment
        ])
        def httpResponse = http.post("https://${this.repositoryUrl}/${pullRequestId}/comments", 'application/json', dataJson)

        script.echo "Adding comment yields http_code: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "201") {
            script.unstable 'Could not add comment'
        }
    }

    /**
     * @return SCM-Manager's representation of PRs. Basically a list of PR objects.
     *  properties (as of SCM-Manager 2.12.0)
     *  * id
     *  * author
     *    * id
     *    * displayName
     *    * mail
     *  * source - the source branch
     *  * target - the target branch
     *  * title 
     *  * description (branch)
     *  * creationDate: (e.g. "2020-10-09T15:08:11.459Z")
     *  * lastModified"
     *  * status, e.g. "OPEN"
     *  * reviewer (list)
     *  * tasks
     *    * todo (number)
     *    * done (number
     *  * tasks sourceRevision
     *  * targetRevision
     *  * targetRevision 
     *  * markedAsReviewed (list)
     *  * emergencyMerged
     *  * ignoredMergeObstacles
     */
    protected getPullRequests() {
        def httpResponse = http.get("https://${this.repositoryUrl}", 'application/vnd.scmm-pullRequestCollection+json;v=2')

        script.echo "Getting all pull requests yields httpCode: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "200") {
            script.unstable 'Could not create pull request'
        }

        def prsAsJson = script.readJSON text: httpResponse.body
        return prsAsJson._embedded.pullRequests
    }

}
