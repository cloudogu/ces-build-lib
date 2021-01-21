package com.cloudogu.ces.cesbuildlib

class SCMManager implements Serializable{

  private script
  private credentials
  Sh sh
  String repositoryUrl

  SCMManager(script, credentials) {
    this.script = script
    this.sh = new Sh(script)
    this.credentials = credentials
  }

  protected String executeWithCredentials(Closure closure) {
    if (credentials) {
      script.withCredentials([script.usernamePassword(credentialsId: credentials,
          passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USER')]) {
        closure.call(true)
      }
    } else {
      closure.call(false)
    }
  }

  protected String getCurlAuthParam() {
    String curlAuthParam = ""
    if(credentials) {
      executeWithCredentials {
        curlAuthParam = "-u ${script.env.GIT_USER}:${script.env.GIT_PASSWORD}"
      }
    }
    return curlAuthParam
  }

  Object searchPullRequestByTitle(String title) {
    def pullRequest
    for (Map pr : getPullRequests()) {
      if (pr["title"] == title) {
        pullRequest = pr
      }
    }

    return pullRequest
  }

  Object getPullRequests() {
    String curlCommand = """curl -w "%{http_code}" ${getCurlAuthParam()} -H 'Content-Type: application/vnd.scmm-pullRequestCollection+json;v=2' https://${this.repositoryUrl}"""
    String httpResponse = script.sh returnStdout: true, script: curlCommand

    //get only the http code (3 characters) of the response.
    String httpCode = httpResponse.reverse().take(3).reverse()
    //drop the http code (last 3 characters) of this string
    String pullRequests = httpResponse.reverse().drop(3).reverse()

    httpCode = httpCode.trim()
    script.echo "Getting all pull requests yields httpCode: ${httpCode}"
    if (httpCode != "200") {
      script.unstable 'Could not create pull request'
    }

    def prsAsJson = script.readJSON text: pullRequests
    return prsAsJson["_embedded"]["pullRequests"]
  }

  String createPullRequest(String source, String target, String title, String description) {
    String data = """{"title": "${title}", "description": "${description}", "source": "${source}", "target": "${target}"}"""
    String curlCommand = """curl -i -X POST ${getCurlAuthParam()} -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '${data}' https://${this.repositoryUrl}"""
    String header = script.sh returnStdout: true, script: curlCommand

    String[] splitHeader = header.split("\n")

    String httpCode = ""
    for(String line: splitHeader) {
      if(line.contains("HTTP/2")){
        String[] httpCodeFull = line.split(" ")
        httpCode = httpCodeFull[1]
      }
    }

    httpCode = httpCode.trim()
    script.echo "Creating pull request yields httpCode: ${httpCode}"
    if (httpCode != "201") {
      script.unstable 'Could not create pull request'
    }

    String pullRequestId = ""
    for(String line: splitHeader) {
      if(line.contains("location:")){
        String[] httpCodeFull = line.split("/")
        pullRequestId = httpCodeFull[httpCodeFull.size() - 1]
        pullRequestId.trim()
      }
    }

    return pullRequestId
  }

  void updateDescription(String pullRequestId, String title, String description) {
    // In order to update the description put in also the title. Otherwise the title is overwritten with an empty string.
    String data = """{"title": "${title}","description": "${description}"}"""
    String curlCommand = """curl -X PUT -w "%{http_code}" ${getCurlAuthParam()} -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '${data}' https://${this.repositoryUrl}/${pullRequestId}"""
    String httpCode = script.sh returnStdout: true, script: curlCommand

    httpCode = httpCode.trim()
    script.echo "Description update yields http_code: ${httpCode}"
    if (httpCode != "204") {
      script.unstable 'Could not update description'
    }
  }

  void addComment(String pullRequestId, String comment) {
    String data = """{"comment": "${comment}"}"""
    String curlCommand = """curl -X POST -w "%{http_code}" ${getCurlAuthParam()} -H 'Content-Type: application/json' -d '${data}' https://${this.repositoryUrl}/${pullRequestId}/comments"""
    String httpCode = script.sh returnStdout: true, script: curlCommand

    httpCode = httpCode.trim()
    script.echo "Adding comment yields http_code: ${httpCode}"
    if (httpCode != "201") {
      script.unstable 'Could not add comment'
    }
  }

}
