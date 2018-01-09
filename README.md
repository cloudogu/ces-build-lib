![Cloudogu logo](https://cloudogu.com/images/logo.png)
# ces-build-lib

Jenkins Pipeline Shared library, that contains additional features for Git, Maven, etc. in an object-oriented manner as well as some additional pipeline steps.

# Usage

* Install [Pipeline: GitHub Groovy Libraries](https://wiki.jenkins.io/display/JENKINS/Pipeline+GitHub+Library+Plugin)
* Use the Library in any Jenkinsfile like so
```
@Library('github.com/cloudogu/ces-build-lib@6cd41e0')
import com.cloudogu.ces.cesbuildlib.*
```
* Best practice: Use a defined version (e.g. a commit, such as `6cd41e0` in the example above) and not a branch such as `develop`. Otherwise your build might change when the there is a new commit on the branch. Using branches is like using snapshots!
* When build executors are docker containers and you intend to use their Docker host in the Pipeline: Please see [#8](https://github.com/cloudogu/ces-build-lib/issues/8#issuecomment-353584252).

# Maven

## Maven from local Jenkins tool

Run maven from a local tool installation on Jenkins.

See [MavenLocal](src/com/cloudogu/ces/cesbuildlib/MavenLocal.groovy)

```
def mvnHome = tool 'M3'
def javaHome = tool 'JDK8'
Maven mvn = new Maven(this, mvnHome, javaHome)

stage('Build') {
    mvn 'clean install'
}
```

## Maven in Docker

Run maven in a docker container. This can be helpful, 
* when constant ports are bound during the build that cause port conflicts in concurrent builds. For example, when running integration tests, unit tests that use infrastructure that binds to ports or
* when one maven repo per builds is required For example when concurrent builds of multi module project install the same snapshot versions. 

The builds are run inside the official maven containers from [Dockerhub](https://hub.docker.com/_/maven/)

See [MavenInDocker](src/com/cloudogu/ces/cesbuildlib/MavenInDocker.groovy)

```
Maven mvn = new MavenInDocker(this, "3.5.0-jdk-8")

stage('Build') {
    mvn 'clean install'
}
```

If you run Docker from your maven build, because you use the [docker-maven-plugin](https://github.com/fabric8io/docker-maven-plugin) for example, you can connect the docker socket through to your docker in maven like so:

```
stage('Unit Test') {
    // The UI module build runs inside a docker container, so pass the docker host to the maven container
    mvn.enableDockerHost = true

    mvn docker:start 

    // Don't expose docker host more than necessary
    mvn.enableDockerHost = false
}
```
There are some security-related concerns about this. See [Docker](#docker).

If you would like to use Jenkin's local maven repo (or more accurate the one of the build executor, typically at `/home/jenkins/.m2`) instead of a maven repo per job (within each workspace), you can use the following option.
```
Maven mvn = new MavenInDocker(this, "3.5.0-jdk-8")
mvn.useLocalRepoFromJenkins = true
```

This speed speeds up the first build and uses less memory. 
However, concurrent builds of multi module projects building the same version (e.g. a SNAPSHOT), might overwrite their dependencies, causing non-deterministic build failures.

## Deploy to nexus repository (e.g. maven central)

ces-build-lib makes deploying to nexus repositories easy, even when it includes signing of the artifacts and usage of 
the nexus staging plugin (as necessary for maven central or other nexus repository pro instances).
 
The most simple case is to deploy to a nexus repo (*not* maven central):
 
* Just set the repository using `Maven.setDeploymentRepository()` passing a repository ID (you can choose), the URL as 
  well as a nexus username and password/access token as jenkins username and password credential.
* Call `Maven.deployToNexusRepository()`. And that is it. 

Simple Example: 
```
mvn.setDeploymentRepository('ces', 'https://ecosystem.cloudogu.com', 'nexusSystemUserCredential')
mvn.deployToNexusRepository()    
```

Note that if the pom.xml's version contains `-SNAPSHOT`, the artifacts are automatically deployed to the 
`snapshot` repository. Otherwise, the artifacts are deployed to the `release` repository.

If you want to sign the artifacts before deploying, just set the credentials for signing before deploying, using 
`Maven.setSignatureCredentials()` passing the secret key as ASC file (as jenkins secret file credential) and the 
passphrase (as jenkins secret text credential).
An ASC file can be exported via  `gpg --export-secret-keys -a ABCDEFGH > secretkey.asc`.
See [Working with PGP Signatures](http://central.sonatype.org/pages/working-with-pgp-signatures.html)

Another option is to use the nexus-staging-maven-plugin instead of the default maven-deploy-plugin.
This is useful if you deploy to a nexus repository pro, such as maven central. 

Just use the `Maven.deployToNexusRepositoryWithStaging()` instead of `Maven.deployToNexusRepository()`.

When deploying to maven central, make sure that your `pom.xml` adheres to the requirements by maven central, as stated
[here](http://central.sonatype.org/pages/requirements.html).
 
Summing up, here is an example for deploying to maven central:

```
mvn.setDeploymentRepository('ossrh', 'https://oss.sonatype.org/', 'mavenCentral-UsernameAndAcccessTokenCredential')
mvn.setSignatureCredentials('mavenCentral-secretKey-asc-file','mavenCentral-secretKey-Passphrase')
mvn.deployToNexusRepositoryWithStaging()            
```

For an example see [triologygmbh/command-bus](https://github.com/triologygmbh/command-bus).

Another option for `deployToNexusRepositoryWithStaging()` and `deployToNexusRepository()` is to pass additional maven 
arguments to the deployment like so: `mvn.deployToNexusRepositoryWithStaging('-X')` (enables debug output).

## Maven Utilities

Available from both local Maven and Maven in Docker.

* `mvn.getVersion()`
* `mvn.getProperty('project.build.sourceEncoding')`

See [Maven](src/com/cloudogu/ces/cesbuildlib/MavenInDocker.groovy)

# Git

```
Git git = new Git(this)

stage('Checkout') {
  git 'https://your.repo'
  /* Don't remove folders starting in "." like .m2 (maven), .npm, .cache, .local (bower), etc. */
  git.clean('".*/"')
}
```

# Docker

The `Docker`class provides the default methods of the global docker variable provided by [docker plugin](https://github.com/jenkinsci/docker-workflow-plugin):

## `Docker` methods provided by the docker plugin
 
 * `withRegistry(url, credentialsId = null, Closure body)`: Specifies a registry URL such as `https://docker.mycorp.com/`, plus an optional credentials ID to connect to it.   
   Example:
     ```groovy
     def dockerImage = docker.build("image/name:1.0", "folderOfDockfile")
     docker.withRegistry("https://your.registry", 'credentialsId') {
       dockerImage.push()
     }
     ```
 * `withServer(uri, credentialsId = null, Closure body)`: Specifies a server URI such as `tcp://swarm.mycorp.com:2376`, plus an optional credentials ID to connect to it.
 * `withTool(toolName, Closure body)`: Specifies the name of a Docker installation to use, if any are defined in Jenkins global configuration. 
    If unspecified, docker is assumed to be in the `$PATH` of the Jenkins agent.
 * `image(id)`: Creates an Image object with a specified name or ID.   
    Example:
     ```groovy
      docker.image('google/cloud-sdk:164.0.0').inside("-e HOME=${pwd()}") {
           sh "echo something"
        }
      ```
     The image returned by the `Docker` class has additional features see bellow.
 * `build(image, args)`: Runs docker build to create and tag the specified image from a Dockerfile in the current directory.
    Additional args may be added, such as `'-f Dockerfile.other --pull --build-arg http_proxy=http://192.168.1.1:3128 .'`.
    Like docker build, args must end with the build context.  
    Example:
     ```groovy
     def dockerContainer = docker.build("image/name:1.0", "folderOfDockfile").run("-e HOME=${pwd()}")
     ```
## Additional features provided by the `Docker` class

The `Docker` class provides additional convenience features:

 * `String findIp(container)` returns the IP address for a docker container instance
 * `String findEnv(container)` returns the environment variables set within the docker container as string
 * `boolean isRunning(container)` return `true` if the container is in state running, otherwise `false`
 
 Example from Jenkinsfile:
 ```groovy
 Docker docker = new Docker(this)
 def dockerContainer = docker.build("image/name:1.0").run()
 waitUntil {
     sleep(time: 10, unit: 'SECONDS')
     return docker.isRunning(dockerContainer)
 }
 echo docker.findIp(dockerContainer)
 echo docker.findEnv(dockerContainer)
 
```

## `Docker.Image` methods provided by the docker plugin

* `id`: The image name with optional tag (mycorp/myapp, mycorp/myapp:latest) or ID (hexadecimal hash).
* `inside(String args = '', Closure body) `: Like `withRun` this starts a container for the duration of the body, but
  all external commands (sh) launched by the body run inside the container rather than on the host. These commands run in
  the same working directory (normally a Jenkins agent workspace), which means that the Docker server must be on localhost.
* `pull`: Runs docker pull. Not necessary before `run`, `withRun`, or `inside`.
* `run(String args = '', String command = "")`:  Uses `docker run` to run the image, and returns a Container which you 
  could stop later. Additional args may be added, such as `'-p 8080:8080 --memory-swap=-1'`. Optional command is 
  equivalent to Docker command specified after the `image()`. Records a run fingerprint in the build.
* `withRun(String args = '', String command = "", Closure body)`:  Like `run` but stops the container as soon as its 
   body exits, so you do not need a try-finally block.
* `tag(String tagName = image().parsedId.tag, boolean force = true)`: Runs docker tag to record a tag of this image 
  (defaulting to the tag it already has). Will rewrite an existing tag if one exists.
* `push(String tagName = image().parsedId.tag, boolean force = true)`: Pushes an image to the registry after tagging it 
  as with the tag method. For example, you can use `image().push 'latest'` to publish it as the latest version in its 
  repository.

## Additional features provided by the `Docker.Image` class

* `mountJenkinsUser()`: Setting this to `true` provides the user that executes the build within docker container's `/etc/passwd`.
  This is necessary for some commands such as npm, ansible, git, id, etc. Those might exit with errors withouta user 
  present.
    
  Why?  
  Note that Jenkins starts Docker containers in the pipeline with the -u parameter (e.g. `-u 1042:1043`).
  That is, the container does not run as root (which is a good thing from a security point of view).
  However, the userID/UID (e.g. `1042`) and the groupID/GID (e.g. `1043`) will most likely not be present within the
  container which causes errors in some executables. 
    
  How?  
  Setting this will cause the creation of a `passwd` file that is mounted into a container started from this `image()`
  (triggered by `run()`, `withRun()` and `inside()` methods). This `passwd` file contains the username, UID, GID of the
  user that executes the build and also sets the current workspace as `HOME` within the docker container.
   
* `mountDockerSocket()`: Setting this to `true` mounts the docker socket into the container.  
   This allows the container to start other containers "next to" itself, that is "sibling" containers. Note that this is 
   similar but not the same as "Docker In Docker".   
     
   Note that this will make the docker host socket accessible from within the the container. Use this wisely. [Some people say](https://dzone.com/articles/never-expose-docker-sockets-period),
   you should not do this at all. On the other hand, the alternative would be to run a real docker host in docker a 
   docker container, aka "docker in docker" or "dind" (which [is possible](https://blog.docker.com/2013/09/docker-can-now-run-within-docker/).
   On this, however, [other people say](http://jpetazzo.github.io/2015/09/03/do-not-use-docker-in-docker-for-ci/), you 
   should not do this at all. So lets stick to mounting the socket, which seems to cause less problems.
   
   This is also used by [MavenInDocker](src/com/cloudogu/ces/cesbuildlib/MavenInDocker.groovy)

Example:
```groovy
new Docker(this).image('docker') // contains the docker client binary
    .mountJenkinsUser()
    .mountDockerSocket()
    .inside() {
        sh 'whoami' // Would fail without mountJenkinsUser = true
        sh 'id' // Would fail without mountJenkinsUser = true
        
        // Start a "sibling" container and wait for it to return
        sh 'docker run hello-world' // Would fail without mountDockerSocket = true 
        
    }
  ```
  
# SonarQube

The [SonarQube Plugin for Jenkins](https://wiki.jenkins.io/display/JENKINS/SonarQube+plugin) provides utility
steps for Jenkins Pipelines. However, analysing and checking the Quality Goal includes some challenges that are solved 
using ces-build-lib's `SonarQube` class:

* Setting the branch name
* Preview analysis for PullRequests
* Updating commit status in GitHub for PullRequests
* Using the SonarQube branch plugin (SonarQube 6.x, developer edition and sonarcloud.io)

The most simple setup will look like this:

For now, `SonarQube` can only analyze using `Maven`. Extending this to use the plain SonarQube Runner in future, should be easy, however.

```groovy
stage('Statical Code Analysis') {
  def sonarQube = new SonarQube(this, 'sonarQubeServerSetupInJenkins')

  sonarQube.analyzeWith(new MavenInDocker(this, "3.5.0-jdk-8"))

  if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
    currentBuild.result ='UNSTABLE'
  }
}
```
Note that
 
* this requires a SonarQube server `sonarQubeServerSetupInJenkins` setup up in your Jenkins instance. You can do this here: `https://yourJenkinsInstance/configure`.
* Calling `waitForQualityGateWebhookToBeCalled()` requires a WebHook to be setup in your SonarQube server (globally or per project), that notifies Jenkins (url: `https://yourJenkinsInstance/sonarqube-webhook/`). See [SonarQube Scanner for Jenkins](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins#AnalyzingwithSonarQubeScannerforJenkins-AnalyzinginaJenkinspipeline). 
* Calling `waitForQualityGateWebhookToBeCalled()` will only work when an analysis has been performed in the current job, i.e. `analyzeWith()` has been called.

## Branches

By default, the `SonarQube` class uses the old logic, of passing the branch name to SonarQube, which will create on project per branch. This is deprecated from SonarQube 6.x, but the alternative is the paid-version-only [Branch Plugin](https://docs.sonarqube.org/display/PLUG/Branch+Plugin).
You can enable the branch plugin like so:

```groovy
sonarQube.isUsingBranchPlugin = true
sonarQube.analyzeWith(mvn)
```

Note that using the branch plugin requires a first analysis without branches. You can achieve this by setting this in the firt run:
```groovy
sonarQube.isIgnoringBranches = true
sonarQube.analyzeWith(mvn)
```
Recommendation: Use Jenkins' replay feature for this. Then commit the `Jenkinsfile` with `isUsingBranchPlugin`.
 

## PullRequests

If `isPullRequest()` is true, `SonarQube.analyzeWith()` will only perform a preview analysis. That is, the results are not sent to the server.
When using the [GitHub Plugin for SonarQube](https://docs.sonarqube.org/display/PLUG/GitHub+Plugin), you can add the results to the PullRequest.
To do so, `SonarQube` needs credentials for the GitHub repo, defined in Jenkins. Please see [here](https://docs.sonarqube.org/display/PLUG/GitHub+Plugin) how to create those in GitHub.
Then save the GitHub access token as secret text in Jenkins at

* `https://yourJenkinsInstance/credentials/` or
* `https://yourJenkinsInstance/job/yourJob/credentials/`.

Finally pass the credentialsId to `SonarQube` in your pipleine like so

```groovy
sonarQube.updateAnalysisResultOfPullRequestsToGitHub('sonarqube-gh')
sonarQube.analyzeWith(mvn)
```

# Steps

## mailIfStatusChanged

Provides the functionality of the Jenkins Post-build Action "E-mail Notification" known from freestyle projects.

```
catchError {
 // Stages and steps
}
mailIfStatusChanged('a@b.cd,123@xy.z')
```
See [mailIfStatusChanged](vars/mailIfStatusChanged.groovy)

## isPullRequest

Returns <code>true</code> if the current build is a pull request (when the `CHANGE_ID`environment variable is set) 
Tested with GitHub.

```
stage('SomethingToSkipWhenInPR') {
    if (!isPullRequest()) {
      // ...
    }
}
```

# Examples
  * This library is built using itself! See [Jenkinsfile](Jenkinsfile)
  * [cloudugo/cas](https://github.com/cloudogu/cas)
  * [triologygmbh/command-bus](https://github.com/triologygmbh/command-bus)
