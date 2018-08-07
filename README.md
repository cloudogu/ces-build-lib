![Cloudogu logo](https://cloudogu.com/images/logo.png)
# ces-build-lib

Jenkins Pipeline Shared library, that contains additional features for Git, Maven, etc. in an object-oriented manner as well as some additional pipeline steps.

# Table of contents
<!-- Update with `doctoc --notitle README.md`. See https://github.com/thlorenz/doctoc -->
<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [Usage](#usage)
- [Syntax completion](#syntax-completion)
- [Maven](#maven)
  - [Maven from local Jenkins tool](#maven-from-local-jenkins-tool)
  - [Maven Wrapper](#maven-wrapper)
  - [Maven in Docker](#maven-in-docker)
  - [Deploy to nexus repository (e.g. maven central)](#deploy-to-nexus-repository-eg-maven-central)
  - [Maven Utilities](#maven-utilities)
- [Git](#git)
  - [Credentials](#credentials)
  - [Git Utilities](#git-utilities)
    - [Read Only](#read-only)
    - [Changes to local repository](#changes-to-local-repository)
    - [Changes to remote repository](#changes-to-remote-repository)
- [Docker](#docker)
  - [`Docker` methods provided by the docker plugin](#docker-methods-provided-by-the-docker-plugin)
  - [Additional features provided by the `Docker` class](#additional-features-provided-by-the-docker-class)
  - [`Docker.Image` methods provided by the docker plugin](#dockerimage-methods-provided-by-the-docker-plugin)
  - [Additional features provided by the `Docker.Image` class](#additional-features-provided-by-the-dockerimage-class)
- [SonarQube](#sonarqube)
  - [Constructors](#constructors)
  - [A complete example](#a-complete-example)
  - [Branches](#branches)
  - [Sonarcloud](#sonarcloud)
  - [Pull Requests in SonarQube](#pull-requests-in-sonarqube)
- [Steps](#steps)
  - [mailIfStatusChanged](#mailifstatuschanged)
  - [isPullRequest](#ispullrequest)
  - [findEmailRecipients](#findemailrecipients)
  - [findHostName](#findhostname)
- [Examples](#examples)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Usage

* Install [Pipeline: GitHub Groovy Libraries](https://wiki.jenkins.io/display/JENKINS/Pipeline+GitHub+Library+Plugin)
* Use the Library in any Jenkinsfile like so
```
@Library('github.com/cloudogu/ces-build-lib@6cd41e0')
import com.cloudogu.ces.cesbuildlib.*
```
* Best practice: Use a defined version (e.g. a commit, such as `6cd41e0` in the example above) and not a branch such as `develop`. Otherwise your build might change when the there is a new commit on the branch. Using branches is like using snapshots!
* When build executors are docker containers and you intend to use their Docker host in the Pipeline: Please see [#8](https://github.com/cloudogu/ces-build-lib/issues/8#issuecomment-353584252).

# Syntax completion

You can get syntax completion in your `Jenkinsfile` when using the ces-build-lib, by adding it as dependency to your project. 

You can get the source.jar from JitPack.

With Maven this can be done like so:

* Define the JitPack repository:
    ```XML
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    ```
* And the ces-build-lib dependency:
    ```XML
    <dependency>
        <!-- Shared Library used in Jenkins. Including this in maven provides code completion in Jenkinsfile. -->
        <groupId>com.github.cloudogu</groupId>
        <artifactId>ces-build-lib</artifactId>
        <!-- Keep this version in sync with the one used in Jenkinsfile -->
        <version>888733b</version>
        <!-- Don't ship this dependency with the app -->
        <optional>true</optional>
        <!-- Don't inherit this dependency! -->
        <scope>provided</scope>
    </dependency>
    ```
Current version is [![](https://jitpack.io/v/cloudogu/ces-build-lib.svg)](https://jitpack.io/#cloudogu/ces-build-lib).<br/> 
For further details and options refer to the [JitPack website](https://jitpack.io/#cloudogu/ces-build-lib).

This is confirmed to work with IntelliJ IDEA.

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

## Maven Wrapper

Run maven using a [Maven Wrapper](https://github.com/takari/maven-wrapper) from the local repository.

```
Maven mvn = new MavenWrapper(this)

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
 
The most simple use case is to deploy to a nexus repo (*not* maven central):
 
* Just set the repository using `Maven.useDeploymentRepository()` passing a repository ID (you can choose), the URL as 
  well as a nexus username and password/access token as jenkins username and password credential.
* Call `Maven.deployToNexusRepository()`. And that is it. 

Simple Example: 
```
mvn.useDeploymentRepository([id: 'ces', url: 'https://ecosystem.cloudogu.com/nexus', credentialsId: 'nexusSystemUserCredential', type: 'Nexus3'])
mvn.deployToNexusRepository()    
```

Right now, the two supported repository types are `Nexus2` and `Nexus3`.

Note that if the pom.xml's version contains `-SNAPSHOT`, the artifacts are automatically deployed to the 
snapshot repository ([e.g. on oss.sonatype.org](https://oss.sonatype.org/content/repositories/snapshots/)). Otherwise, 
the artifacts are deployed to the release repository ([e.g. on oss.sonatype.org](https://oss.sonatype.org/content/repositories/releases/)).

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

Note that as of nexus-staging-maven-plugin version 1.6.8, it [does seem to read the distribution repositories from pom.xml only](https://issues.sonatype.org/browse/NEXUS-15464).

That is, you need to specify them in your pom.xml, they cannot be passed by the ces-build-lib. So for example for maven 
central you need to add the following:

```xml
<distributionManagement>
    <snapshotRepository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </snapshotRepository>
    <repository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
</distributionManagement>
```
The repository ID (here: `ossrh`) and the base nexus URL (here: `https://oss.sonatype.org`) must match the one passed
to ces-build-lib using `useDeploymentRepository()`.

Summing up, here is an example for deploying to maven central:

```
mvn.useDeploymentRepository([id: 'ossrh', url: 'https://oss.sonatype.org', credentialsId: 'mavenCentral-UsernameAndAcccessTokenCredential', type: 'Nexus2'])
mvn.setSignatureCredentials('mavenCentral-secretKey-asc-file','mavenCentral-secretKey-Passphrase')
mvn.deployToNexusRepositoryWithStaging()            
```

Note that the staging of releases might well take 10 minutes. After that, the artifacts are in the 
[release repository](https://oss.sonatype.org/content/repositories/releases/), which is *later* (feels like nightly) 
synced to maven central.  

For an example see [triologygmbh/command-bus](https://github.com/triologygmbh/command-bus).

Another option for `deployToNexusRepositoryWithStaging()` and `deployToNexusRepository()` is to pass additional maven 
arguments to the deployment like so: `mvn.deployToNexusRepositoryWithStaging('-X')` (enables debug output).

## Maven Utilities

Available from both local Maven and Maven in Docker.

* `mvn.getVersion()`
* `mvn.getProperty('project.build.sourceEncoding')`

See [Maven](src/com/cloudogu/ces/cesbuildlib/MavenInDocker.groovy)

# Git

An extension to the `git` step, that provides an API for some commonly used git commands and utilities.
Mostly, this is a convenient wrapper around using the `sh 'git ...'` calls.

Example: 

```groovy
Git git = new Git(this)

stage('Checkout') {
  git 'https://your.repo'
  /* Don't remove folders starting in "." like .m2 (maven), .npm, .cache, .local (bower), etc. */
  git.clean('".*/"')
}
```

## Credentials

You can optionally pass `usernamePassword` credentials to `Git` during construction. These are then used for cloning 
and pushing.

```
Git annonymousGit = new Git(this)
Git gitWithCreds = new Git(this, 'ourCredentials')


annonymousGit 'https://your.repo'
gitWithCreds 'https://your.repo' // Implicitly passed credentials
```

## Git Utilities

### Read Only

* `git.clean()` - Removes all untracked and unstaged files.
* `git.clean('".*/"')` - Removes all untracked and unstaged files, except folders starting in "." like .m2 (maven), 
  .npm, .cache, .local (bower), etc.
* `git.branchName` - e.g. `feature/xyz/abc`
* `git.simpleBranchName` - e.g. `abc`
* `git.commitAuthorComplete` -  e.g. `User Name <user.name@doma.in>`
* `git.commitAuthorEmail` -  e.g. `user.name@doma.in`
* `git.commitAuthorName` -  e.g. `User Name`
* `git.commitHash` -  e.g. `fb1c8820df462272011bca5fddbe6933e91d69ed`
* `git.commitHashShort` -  e.g. `fb1c882`
* `git.repositoryUrl` -  e.g. `https://github.com/orga/repo.git`
* `git.gitHubRepositoryName` -  e.g. `orga/repo`
* `git.tag` -  e.g. `1.0.0` or `undefined` if not set
* `git.isTag()` - is there a tag on the current commit?

### Changes to local repository

* `git.add('.')`
* `git.commit('message', 'Author', 'Author@mail.server)`
* `git.commit('message')` - uses the name and email of the last committer as author and committer.

### Changes to remote repository

* `git.push('master')` - pushes origin
* `pushGitHubPagesBranch('folderToPush', 'commit Message')` - Commits and pushes a folder to the `gh-pages` branch of 
   the current repo. Can be used to conveniently deliver websites. See https://pages.github.com. Note:
   * Uses the name and email of the last committer as author and committer.
   * the `gh-pages` branch is temporarily checked out to the `.gh-pages` folder.
   * Don't forget to create a git object with credentials.
   * Example: [cloudogu/continuous-delivery-slides-example](https://github.com/cloudogu/continuous-delivery-slides-example/) 


# Docker

The `Docker` class provides the default methods of the global docker variable provided by [docker plugin](https://github.com/jenkinsci/docker-workflow-plugin):

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
   
* `installDockerClient(String version)`: Installs the docker client with the specified version inside the container.  
   This can be called in addition to mountDockerSocket(), when the "docker" CLI is required on the PATH.  
   
   For available versions see [here](https://download.docker.com/linux/static/stable/x86_64/).
   For an exampl see [here](https://github.com/cloudogu/continuous-delivery-docs-example) 

Examples:

Docker Container that uses its own docker client:
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

Docker container that does not have its own docker client
```groovy
new Docker(this).image('kkarczmarczyk/node-yarn:8.0-wheezy')
    .mountJenkinsUser()
    .mountDockerSocket()
    .installDockerClient('17.12.1')
    .inside() {
        // Start a "sibling" container and wait for it to return
        sh 'docker run hello-world' // Would fail without mountDockerSocket = true & installDockerClient()
    }
```

# SonarQube

When analyzing code with SonarQube there are a couple of challenges that are solved using ces-build-lib's 
`SonarQube` class:

* Setting the branch name (note that this only works in Jenkins multi-branch pipeline builds, regular pipelines don't have information about branches - see #11)
* Analysis for Pull Requests
* Commenting on Pull Requtests
* Updating commit status in GitHub for Pull Requests
* Using the SonarQube branch plugin (SonarQube 6.x, developer edition and sonarcloud.io)

## Constructors

In general, you can analyse with or without the [SonarQube Plugin for Jenkins](https://wiki.jenkins.io/display/JENKINS/SonarQube+plugin):

* `new SonarQube(this, [sonarQubeEnv: 'sonarQubeServerSetupInJenkins'])` requires the SonarQube plugin and the 
  SonarQube server `sonarQubeServerSetupInJenkins` setup up in your Jenkins instance. You can do this here: 
  `https://yourJenkinsInstance/configure`.
* `new SonarQube(this, [token: 'secretTextCred', sonarHostUrl: 'http://ces/sonar'])` does not require the plugin 
  and uses an access token, stored as secret text credential `secretTextCred` in your Jenkins instance.   
* `new SonarQube(this, [usernamePassword: 'usrPwCred', sonarHostUrl: 'http://ces/sonar'])` does not require the 
   plugin and uses a SonarQube user account, stored as username with password credential `usrPwCred` in your Jenkins 
   instance.

With the `SonarQube` instance you can now analyze your code. When using the plugin (i.e. `sonarQubeEnv`) you can also
wait for the quality gate status, that is computed by SonarQube asynchronously. Note that this does **not** work for `token`
and `usernamePassword`.
 
## A complete example

```groovy
stage('Statical Code Analysis') {
  def sonarQube = new SonarQube(this, [sonarQubeEnv: 'sonarQubeServerSetupInJenkins'])

  sonarQube.analyzeWith(new MavenInDocker(this, "3.5.0-jdk-8"))

  if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
    currentBuild.result ='UNSTABLE'
  }
}
```

Note that
 
* Calling `waitForQualityGateWebhookToBeCalled()` requires a WebHook to be setup in your SonarQube server (globally or 
  per project), that notifies Jenkins (url: `https://yourJenkinsInstance/sonarqube-webhook/`).  
  See [SonarQube Scanner for Jenkins](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins#AnalyzingwithSonarQubeScannerforJenkins-AnalyzinginaJenkinspipeline). 
* Calling `waitForQualityGateWebhookToBeCalled()` will only work when an analysis has been performed in the current job,
  i.e. `analyzeWith()` has been called and in conjuction with `sonarQubeEnv`.
* When used in conjunction with [SonarQubeCommunity/sonar-build-breaker](https://github.com/SonarQubeCommunity/sonar-build-breaker),
  `waitForQualityGateWebhookToBeCalled()` will fail your build, if quality gate is not passed.
* For now, `SonarQube` can only analyze using `Maven`. Extending this to use the plain SonarQube Runner in future, 
  should be easy, however.
  
## Branches

By default, the `SonarQube` class uses the old logic, of passing the branch name to SonarQube, which will create one 
project per branch. This is deprecated from SonarQube 6.x, but the alternative is the paid-version-only 
[Branch Plugin](https://docs.sonarqube.org/display/PLUG/Branch+Plugin).

You can enable the branch plugin like so:

```groovy
sonarQube.isUsingBranchPlugin = true
sonarQube.analyzeWith(mvn)
```

Note that using the branch plugin **requires a first analysis without branches**.

You can do this on Jenkins or locally.

On Jenkins, you can achieve this by setting the following for the first run:
```groovy
sonarQube.isIgnoringBranches = true
sonarQube.analyzeWith(mvn)
```
Recommendation: Use Jenkins' replay feature for this. Then commit the `Jenkinsfile` with `isUsingBranchPlugin`.

An alternative is running the first analysis locally, e.g. with maven
`mvn clean install sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=YOUR-ORG -Dsonar.login=YOUR-TOKEN`
 
## SonarCloud

[SonarCloud](https://sonarcloud.io) is a public SonarQube instance that has some extra features, such as PullRequest 
decoration for GitHub, BitBucket, etc.
ces-build-lib encapsulates the setup in `SonarCloud` class.
It works just like `SonarQube`, i.e. you can create it using `sonarQubeEnv`, `token`, etc. and it provides the `analyzeWith()` and 
`waitForQualityGateWebhookToBeCalled()` methods.  
The only difference: You either have to pass your organization ID using the `sonarOrganization: 'YOUR_ID'` parameter 
during construction, or configure it under `https://yourJenkinsInstance/configure` as "Additional analysis properties" 
(hit the "Advanced..." button to get there): `sonar.organization=YOUR_ID`.

Example using SonarCloud:
 
```groovy
  def sonarQube = SonarCloud(this, [sonarQubeEnv: 'sonarcloud.io', sonarOrganization: 'YOUR_ID'])

  sonarQube.analyzeWith(new MavenInDocker(this, "3.5.0-jdk-8"))

  if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
    currentBuild.result ='UNSTABLE'
  }
```

Just like for ordinary SonarQube, you have to setup a webhook in SonarCloud for `waitForQualityGateWebhookToBeCalled()` to work (see [above](#a-complete-example)).

If you want SonarCloud to decorate your Pull Requests, you will have to install the [SonarCloud Application for GitHub](https://github.com/apps/sonarcloud) into your GitHub organization or account. See also [Pull Request analysis](https://sonarcloud.io/documentation/pull_request).

Note that SonarCloud uses the Branch Plugin, so the first analysis has to be done differently, as described in [Branches](#branches).

## Pull Requests in SonarQube

As described above, SonarCloud can annotate PullRequests using the SonarCloud Application for GitHub. You can also do this from a regular SonarQube using the [GitHub Plugin for SonarQube](https://docs.sonarqube.org/display/PLUG/GitHub+Plugin).
To do so, `SonarQube` needs credentials for the GitHub repo, defined as Jenkins credentials. Please see [here](https://docs.sonarqube.org/display/PLUG/GitHub+Plugin) how to create those in GitHub.
Then save the GitHub access token as secret text in Jenkins at

* `https://yourJenkinsInstance/credentials/` or
* `https://yourJenkinsInstance/job/yourJob/credentials/`.

Finally pass the credentialsId to `SonarQube` in your pipleine like so

```groovy
sonarQube.updateAnalysisResultOfPullRequestsToGitHub('sonarqube-gh')
sonarQube.analyzeWith(mvn)
```

Note: When analysing the Pull Request using the `SonarQube` class, `SonarQube.analyzeWith()` will only perform a preview analysis. That is, the results are not sent to the server.

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

## findEmailRecipients

Determines the email recipients:
For branches that are considered unstable (all except for 'master' and 'develop') only the Git author is returned
(if present).
Otherwise, the default recipients (passed as parameter) and git author are returned.

```
catchError {
 // Stages and steps
}
mailIfStatusChanged(findEmailRecipients('a@b.cd,123@xy.z'))
```
The example writes state changes email to 'a@b.cd,123@xy.z' + git author for stable branches and only to git author 
for unstable branches.

## findHostName

Returns the hostname of the current Jenkins instance. 
For example, ff running on `http(s)://server:port/jenkins`, `server` is returned.


# Examples
  * This library is built using itself! See [Jenkinsfile](Jenkinsfile)
  * [cloudugo/cas](https://github.com/cloudogu/cas)
  * [triologygmbh/command-bus](https://github.com/triologygmbh/command-bus)
