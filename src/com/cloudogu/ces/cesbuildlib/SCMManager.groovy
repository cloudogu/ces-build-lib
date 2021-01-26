package com.cloudogu.ces.cesbuildlib

import groovy.json.JsonOutput

class SCMManager implements Serializable{

  private script
  private HttpClient http
  String repositoryUrl

  SCMManager(script, credentials) {
    this.script = script
    this.http = new HttpClient(script, credentials)  
  }

  def searchPullRequestIdByTitle(String title) {
    def pullRequest
    for (Map pr : getPullRequests()) {
      if (pr["title"] == title) {
        pullRequest = pr
      }
    }
    
    if (pullRequest) {
      pullRequest["id"].toString()
    } else {
      return ""
    }
  }

  protected getPullRequests() {
    def httpResponse = http.get("https://${this.repositoryUrl}", 'application/vnd.scmm-pullRequestCollection+json;v=2')
    
    script.echo "Getting all pull requests yields httpCode: ${httpResponse.httpCode}"
    if (httpResponse.httpCode != "200") {
      script.unstable 'Could not create pull request'
    }

    def prsAsJson = script.readJSON text: httpResponse.body
    return prsAsJson["_embedded"]["pullRequests"]
  }

  String createPullRequest(String source, String target, String title, String description) {
    def dataJson = JsonOutput.toJson([
          title: title,
          description: description,
          source: source,
          target: target
    ])
    def httpResponse = http.post("https://${this.repositoryUrl}", 'application/vnd.scmm-pullRequest+json;v=2', dataJson)

    script.echo "Creating pull request yields httpCode: ${httpResponse.httpCode}"
    if (httpResponse.httpCode != "201") {
      script.unstable 'Could not create pull request'
    }

    return httpResponse.headers['location'].split("/")[-1]
  }

  void updateDescription(String pullRequestId, String title, String description) {
    // In order to update the description put in also the title. Otherwise the title is overwritten with an empty string.
    def dataJson = JsonOutput.toJson([
          title: title,
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

}
