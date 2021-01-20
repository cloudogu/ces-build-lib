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
        def auth = "${script.env.GIT_USER}:${script.env.GIT_PASSWORD}"
        closure.call(auth)
      }
    } else {
      closure.call(false)
    }
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
    String pullRequests_with_http_code = ""
    executeWithCredentials { auth ->
      String script = """curl -w "%{http_code}" -u ${auth} -H 'Content-Type: application/vnd.scmm-pullRequestCollection+json;v=2' https://${this.repositoryUrl}"""
      pullRequests_with_http_code = this.script.sh returnStdout: true, script: script
    }
    //get only the http code (3 characters) of the response.
    String http_code = pullRequests_with_http_code.reverse().take(3).reverse()
    //drop the http code (last 3 characters) of this string
    String pullRequests = pullRequests_with_http_code.reverse().drop(3).reverse()

    http_code = http_code.trim()
    this.script.echo "Getting all pull requests yields http_code: ${http_code}"
    if (http_code != "200") {
      unstable 'Could not create pull request'
    }

    def prsAsJson = readJSON text: pullRequests
    return prsAsJson["_embedded"]["pullRequests"]
  }

  String createPullRequest(String source, String target, String title, String description) {
    def data = """{"title": "${title}", "description": "${description}", "source": "${source}", "target": "${target}"}"""

    String header = ""
    executeWithCredentials { auth ->
      def script = """curl -i -X POST -u ${auth} -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '${data}' https://${this.repositoryUrl}"""
      header = this.script.sh returnStdout: true, script: script
    }
    String[] splitHeader = header.split("\n")

    String http_code
    for(String line: splitHeader) {
      if(line.contains("HTTP/2")){
        String[] http_code_full = line.split(" ")
        http_code = http_code_full[1]
      }
    }

    http_code = http_code.trim()
    this.script.echo "Creating pull request yields http_code: ${http_code}"
    if (http_code != "201") {
      unstable 'Could not create pull request'
    }

    String pullRequestId
    for(String line: splitHeader) {
      if(line.contains("location:")){
        String[] http_code_full = line.split("/")
        pullRequestId = http_code_full[http_code_full.size() - 1]
        pullRequestId.trim()
      }
    }

    return pullRequestId
  }

  void updateDescription(String pullRequestId, String title, String description) {
    // In order to update the description put in also the title. Otherwise the title is overwritten with an empty string.
    def data = """{"title": "${title}","description": "${description}"}"""
    String http_code = ""
    executeWithCredentials { auth ->
      def script = """curl -X PUT -w "%{http_code}" -u ${auth} -H 'Content-Type: application/vnd.scmm-pullRequest+json;v=2' -d '${data}' https://${this.repositoryUrl}/${pullRequestId}"""
      http_code = this.script.sh returnStdout: true, script: script
    }

    http_code = http_code.trim()
    this.script.echo "Description update yields http_code: ${http_code}"
    if (http_code != "204") {
      unstable 'Could not update description'
    }
  }

  void addComment(String pullRequestId, String comment) {
    def data = """{"comment": "${comment}"}"""

    String http_code = ""
    executeWithCredentials { auth ->
      def script = """curl -X POST -w "%{http_code}" -u ${auth} -H 'Content-Type: application/json' -d '${data}' https://${this.repositoryUrl}/${pullRequestId}/comments"""
      http_code = this.script.sh returnStdout: true, script: script
    }

    http_code = http_code.trim()
    this.script.echo "Adding comment yields http_code: ${http_code}"
    if (http_code != "201") {
      unstable 'Could not add comment'
    }
  }

}
