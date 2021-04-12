package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonOutput

class SCMManager implements Serializable {

    private script
    protected HttpClient http
    protected String baseUrl

    SCMManager(script, String baseUrl, String credentials) {
        this.script = script
        this.baseUrl = baseUrl
        this.http = new HttpClient(script, credentials)
    }

    String searchPullRequestIdByTitle(String repository, String title) {
        def pullRequest
        for (Map pr : getPullRequests(repository)) {
            if (pr.title == title) {
                pullRequest = pr
            }
        }

        if (pullRequest) {
            return pullRequest.id.toString()
        } else {
            return ''
        }
    }

    String createPullRequest(String repository, String source, String target, String title, String description) {
        def dataJson = JsonOutput.toJson([
            title      : title,
            description: description,
            source     : source,
            target     : target
        ])
        def httpResponse = http.post(pullRequestEndpoint(repository), 'application/vnd.scmm-pullRequest+json;v=2', dataJson)

        script.echo "Creating pull request yields httpCode: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "201") {
            script.echo 'WARNING: Http status code indicates, that pull request was not created'
            return ''
        }

        // example: "location: https://some/pr/42" - extract id
        // in some cases the location key might be upper case so we check for that
        if (httpResponse.headers.containsKey("location")) {
            return httpResponse.headers.location.split("/")[-1]
        } else {
            return httpResponse.headers.Location.split("/")[-1]
        }
    }

    boolean updatePullRequest(String repository, String pullRequestId, String title, String description) {
        // In order to update the description put in also the title. Otherwise the title is overwritten with an empty string.
        def dataJson = JsonOutput.toJson([
            title      : title,
            description: description
        ])

        def httpResponse = http.put("${pullRequestEndpoint(repository)}/${pullRequestId}", 'application/vnd.scmm-pullRequest+json;v=2', dataJson)

        script.echo "Pull request update yields http_code: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "204") {
            script.echo 'WARNING: Http status code indicates, that the pull request was not updated'
            return false
        }
        return true
    }

    String createOrUpdatePullRequest(String repository, String source, String target, String title, String description) {

        def pullRequestId = searchPullRequestIdByTitle(repository, title)

        if(pullRequestId.isEmpty()) {
            return createPullRequest(repository, source, target, title, description)
        } else {
            if(updatePullRequest(repository, pullRequestId, title, description)) {
                return pullRequestId
            } else {
                return ''
            }
        }
    }

    boolean addComment(String repository, String pullRequestId, String comment) {
        def dataJson = JsonOutput.toJson([
            comment: comment
        ])
        def httpResponse = http.post("${pullRequestEndpoint(repository)}/${pullRequestId}/comments", 'application/json', dataJson)

        script.echo "Adding comment yields http_code: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "201") {
            script.echo 'WARNING: Http status code indicates, that the comment was not added'
            return false
        }
        return true
    }

    protected String pullRequestEndpoint(String repository) {
        "${this.baseUrl}/api/v2/pull-requests/${repository}"
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
    protected List getPullRequests(String repository) {
        def httpResponse = http.get(pullRequestEndpoint(repository), 'application/vnd.scmm-pullRequestCollection+json;v=2')

        script.echo "Getting all pull requests yields httpCode: ${httpResponse.httpCode}"
        if (httpResponse.httpCode != "200") {
            script.echo 'WARNING: Http status code indicates, that the pull requests could not be retrieved'
            return []
        }

        def prsAsJson = script.readJSON text: httpResponse.body
        return prsAsJson._embedded.pullRequests
    }

}
