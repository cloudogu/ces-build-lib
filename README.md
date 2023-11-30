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
    - [With local JDK tool](#with-local-jdk-tool)
    - [With the JDK provided by the build agent](#with-the-jdk-provided-by-the-build-agent)
  - [Maven in Docker](#maven-in-docker)
    - [Plain Maven In Docker](#plain-maven-in-docker)
    - [Maven Wrapper In Docker](#maven-wrapper-in-docker)
    - [Advanced Maven in Docker features](#advanced-maven-in-docker-features)
      - [Maven starts new containers](#maven-starts-new-containers)
      - [Local repo](#local-repo)
        - [Containers](#containers)
        - [Without Containers](#without-containers)
      - [Lazy evaluation / execute more steps inside container](#lazy-evaluation--execute-more-steps-inside-container)
  - [Repository Credentials](#repository-credentials)
  - [Deploying to Nexus repository](#deploying-to-nexus-repository)
    - [Deploying artifacts](#deploying-artifacts)
      - [Simple deployment](#simple-deployment)
      - [Signing artifacts (e.g. Maven Central)](#signing-artifacts-eg-maven-central)
      - [Deploying with staging (e.g. Maven Central)](#deploying-with-staging-eg-maven-central)
    - [Deploying sites](#deploying-sites)
    - [Passing additional arguments](#passing-additional-arguments)
  - [Maven Utilities](#maven-utilities)
- [Gradle](#gradle)
  - [Gradle Wrapper in Docker](#gradle-wrapper-in-docker)
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
- [Dockerfile](#dockerfile)
- [SonarQube](#sonarqube)
  - [Constructors](#constructors)
  - [A complete example](#a-complete-example)
  - [Branches](#branches)
  - [SonarCloud](#sonarcloud)
  - [Pull Requests in SonarQube](#pull-requests-in-sonarqube)
- [Changelog](#changelog)
  - [changelogFileName](#changelogfilename)
- [GitHub](#github)
- [GitFlow](#gitflow)
- [SCM-Manager](#scm-manager)
  - [Pull Requests](#pull-requests)
- [HttpClient](#httpclient)
- [K3d](#k3d)
- [DoguRegistry](#doguregistry)
- [Bats](#bats)
- [Steps](#steps)
  - [mailIfStatusChanged](#mailifstatuschanged)
  - [isPullRequest](#ispullrequest)
  - [findEmailRecipients](#findemailrecipients)
  - [findHostName](#findhostname)
  - [isBuildSuccessful](#isbuildsuccessful)
  - [findVulnerabilitiesWithTrivy](#findvulnerabilitieswithtrivy)
- [Examples](#examples)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Usage

* Install [Pipeline: GitHub Groovy Libraries](https://wiki.jenkins.io/display/JENKINS/Pipeline+GitHub+Library+Plugin)
* Use the Library in any Jenkinsfile like so
```
@Library('github.com/cloudogu/ces-build-lib@1.67.0')
import com.cloudogu.ces.cesbuildlib.*
```
* Best practice: Use a defined version (e.g. a git commit hash or a git tag, such as `6cd41e0` or `1.67.0` in the example above) and not a branch such as `develop`. Otherwise, your build might change when the there is a new commit on the branch. Using branches is like using snapshots!
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
Or you can download the file (and sources) manually and add them to your IDE. For example:

* `https://jitpack.io/com/github/cloudogu/ces-build-lib/9fa7ac4/ces-build-lib-9fa7ac4.jar`
* `https://jitpack.io/com/github/cloudogu/ces-build-lib/9fa7ac4/ces-build-lib-9fa7ac4-sources.jar`

Current version is [![](https://jitpack.io/v/cloudogu/ces-build-lib.svg)](https://jitpack.io/#cloudogu/ces-build-lib).<br/> 
For further details and options refer to the [JitPack website](https://jitpack.io/#cloudogu/ces-build-lib).

This is confirmed to work with IntelliJ IDEA.

# Maven

## Maven from local Jenkins tool

Run maven from a local tool installation on Jenkins.

See [MavenLocal](src/com/cloudogu/ces/cesbuildlib/MavenLocal.groovy)

```
def mvnHome = tool 'M3'
def javaHome = tool 'OpenJDK-8'
Maven mvn = new MavenLocal(this, mvnHome, javaHome)

stage('Build') {
    mvn 'clean install'
}
```

## Maven Wrapper

Run maven using a [Maven Wrapper](https://github.com/takari/maven-wrapper) from the local repository.

### With local JDK tool

Similar to `MavenLocal` you can use the Maven Wrapper with a JDK from a local tool installation on Jenkins:

```
def javaHome = tool 'OpenJDK-8'
Maven mvn = new MavenWrapper(this, javaHome)

stage('Build') {
    mvn 'clean install'
}
```

### With the JDK provided by the build agent

It is also possible to not specify a JDK tool and use the Java Runtime on the Build agent's `PATH`. However, 
experience tells us that this is absolutely non-deterministic and will result in unpredictable behavior.  
So: Better set an explicit Java tool to be used or use `MavenWrapperInDocker.`

```
Maven mvn = new MavenWrapper(this)

stage('Build') {
    mvn 'clean install'
}
```

## Maven in Docker

Run maven in a docker container. This can be helpful, when

* constant ports are bound during the build that cause port conflicts in concurrent builds. For example, when running 
  integration tests, unit tests that use infrastructure that binds to ports or
* one maven repo per builds is required For example when concurrent builds of multi module project install the same 
  snapshot versions.
  
### Plain Maven In Docker

The builds are run inside the official maven containers from [Dockerhub](https://hub.docker.com/_/maven/)

See [MavenInDocker](src/com/cloudogu/ces/cesbuildlib/MavenInDocker.groovy)

```
Maven mvn = new MavenInDocker(this, "3.5.0-jdk-8")

stage('Build') {
    mvn 'clean install'
}
```

### Maven Wrapper In Docker

It's also possible to use the MavenWrapper in a Docker Container. Here, the Docker container is responsible for 
providing the JDK.

See [MavenWrapperInDocker](src/com/cloudogu/ces/cesbuildlib/MavenWrapperInDocker.groovy)

```
Maven mvn = MavenWrapperInDocker(this, 'adoptopenjdk/openjdk11:jdk-11.0.10_9-alpine')

stage('Build') {
    mvn 'clean install'
}
```

Since Oracle's announcement of shorter free JDK support, plenty of JDK images have appeared on public container image 
registries, where `adoptopenjdk` is just one option. The choice is yours. 

### Advanced Maven in Docker features

The following features apply to plain Maven as well as Maven Wrapper in Docker.

#### Maven starts new containers

If you run Docker from your maven build, because you use the 
[docker-maven-plugin](https://github.com/fabric8io/docker-maven-plugin) for example, you can connect the docker socket 
through to your docker in maven like so:

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

#### Local repo

##### Maven in Docker
If you would like to use Jenkin's local maven repo (or more accurate the one of the build executor, typically at `/home/jenkins/.m2`) 
instead of a maven repo per job (within each workspace), you can use the following options:

```
Maven mvn = new MavenInDocker(this, "3.5.0-jdk-8")
mvn.useLocalRepoFromJenkins = true
```

This speed speeds up the first build and uses less memory.
However, concurrent builds of multi module projects building the same version (e.g. a SNAPSHOT), might overwrite their dependencies, causing non-deterministic build failures.

##### Maven without Docker

The default is the default maven behavior `/home/jenkins/.m2` is used.
If you want to use a separate maven repo per Workspace (e.g. in order to avoid concurrent builds overwriting 
dependencies of multi module projects building the same version (e.g. a SNAPSHOT) the following will work:

```groovy
mvn.additionalArgs += " -Dmaven.repo.local=${env.WORKSPACE}/.m2"
```

#### Lazy evaluation / execute more steps inside container
 
If you need to execute more steps inside the maven container you can pass a closure to your maven instance that is 
lazily evaluated within the container. The String value returned are the maven arguments. 

```
Maven mvn = new MavenInDocker(this, "3.5.0-jdk-8"),
echo "Outside Maven Container! ${new Docker(this).findIp()}"
mvn {
    echo "Insinde Maven Container! ${new Docker(this).findIp()}"
    'clean package -DskipTests'
}
```

## Repository Credentials

If you specified one or more `<repository>` in your `pom.xml` that requires authentication, you can pass these 
credentials to your ces-build-lib `Maven` instance like so:

```bash
mvn.useRepositoryCredentials([id: 'ces', credentialsId: 'nexusSystemUserCredential'],
                             [id: 'another', credentialsId: 'nexusSystemUserCredential'])
```

Note that the `id` must match the one specified in your `pom.xml` and the credentials ID must belong to a username and
 password credential defined in Jenkins.

## Deploying to Nexus repository 

### Deploying artifacts

ces-build-lib makes deploying to nexus repositories easy, even when it includes signing of the artifacts and usage of 
the nexus staging plugin (as necessary for Maven Central or other Nexus repository pro instances).
 
#### Simple deployment

The most simple use case is to deploy to a nexus repo (*not* Maven Central):
 
* Just set the repository using `Maven.useRepositoryCredentials()` by passing a nexus username and password/access token
  as jenkins username and password credential and
  * either a repository ID that matches a `<distributionManagement><repository>` (or `<snapshotRepository>`, examples 
    bellow) defined in your `pom.xml` (then, no `url` or `type` parameters are needed)    
    (`distributionManagement` > `snapshotRepository` or `repository` (depending on the `version`) > `id`)
  * or a repository ID (you can choose) and the URL.  
    In this case you can alos specifiy a `type: 'Nexus2'` (defaults to Nexus3) - as the base-URLs differ.
    **This approach is deprecated and might be removed from ces-build-lib in the future.**
* Call `Maven.deployToNexusRepository()`. And that is it. 

Simple Example: 
```
# <distributionManagement> in pom.xml (preferred)
mvn.useRepositoryCredentials([id: 'ces', credentialsId: 'nexusSystemUserCredential'])
# Alternative: Distribution management via Jenkins (deprecated)
mvn.useRepositoryCredentials([id: 'ces', url: 'https://ecosystem.cloudogu.com/nexus', credentialsId: 'nexusSystemUserCredential', type: 'Nexus2'])

# Deploy
mvn.deployToNexusRepository()    
```

Note that if the pom.xml's version contains `-SNAPSHOT`, the artifacts are automatically deployed to the 
snapshot repository ([e.g. on oss.sonatype.org](https://oss.sonatype.org/content/repositories/snapshots/)). Otherwise, 
the artifacts are deployed to the release repository ([e.g. on oss.sonatype.org](https://oss.sonatype.org/content/repositories/releases/)).

#### Signing artifacts (e.g. Maven Central)

If you want to sign the artifacts before deploying, just set the credentials for signing before deploying, using 
`Maven.setSignatureCredentials()` passing the secret key as ASC file (as jenkins secret file credential) and the 
passphrase (as jenkins secret text credential).
An ASC file can be exported via  `gpg --export-secret-keys -a ABCDEFGH > secretkey.asc`.
See [Working with PGP Signatures](http://central.sonatype.org/pages/working-with-pgp-signatures.html)

#### Deploying with staging (e.g. Maven Central)

Another option is to use the nexus-staging-maven-plugin instead of the default maven-deploy-plugin.
This is useful if you deploy to a Nexus repository pro, such as Maven Central. 

Just use the `Maven.deployToNexusRepositoryWithStaging()` instead of `Maven.deployToNexusRepository()`.

When deploying to Maven Central, make sure that your `pom.xml` adheres to the requirements by Maven Central, as stated
[here](http://central.sonatype.org/pages/requirements.html).

Note that as of nexus-staging-maven-plugin version 1.6.8, it 
[does seem to read the distribution repositories from pom.xml only](https://issues.sonatype.org/browse/NEXUS-15464).

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

In addition you either have to pass an `url` to `useRepositoryCredentials()` or specify the nexus-staging-maven plugin in your pom.xml:

```xml
  <plugin>
    <groupId>org.sonatype.plugins</groupId>
    <artifactId>nexus-staging-maven-plugin</artifactId>
    <!-- ... -->
    <configuration>
      <serverId>ossrh</serverId>
      <nexusUrl>https://oss.sonatype.org/</nexusUrl>
    </configuration>
  </plugin>
```
          
Either way, the repository ID (here: `ossrh`) and the base nexus URL (here: `https://oss.sonatype.org`) in 
`distributionManagement` and `nexus-staging-maven plugin` must conform to each other.

Summing up, here is an example for deploying to Maven Central:

```
// url is optional, if described in nexus-staging-maven-plugin in pom.xml 
mvn.useRepositoryCredentials([id: 'ossrh', url: 'https://oss.sonatype.org', credentialsId: 'mavenCentral-UsernameAndAcccessTokenCredential', type: 'Nexus2'])
mvn.setSignatureCredentials('mavenCentral-secretKey-asc-file','mavenCentral-secretKey-Passphrase')
mvn.deployToNexusRepositoryWithStaging()            
```

Note that the staging of releases might well take 10 minutes. After that, the artifacts are in the 
[release repository](https://oss.sonatype.org/content/repositories/releases/), which is *later* (feels like nightly) 
synced to Maven Central.  

For an example see [cloudogu/command-bus](https://github.com/cloudogu/command-bus).

### Deploying sites

Similar to deploying artifacts as described above, we can also easily deploy a [Maven site](https://maven.apache.org/guides/mini/guide-site.html)
to a "raw" maven repository.  

Note that the site plugin [does not provide options to specify the target repository via the command line](http://maven.apache.org/plugins/maven-site-plugin/deploy-mojo.html).
That is, it has to be configured in the pom.xml like so:

```xml
<distributionManagement>
    <site>
        <id>ces</id>
        <name>site repository cloudogu ecosystem</name>
        <url>dav:https://your.domain/nexus/repository/Site-repo/${project.groupId}/${project.artifactId}/${project.version}/</url>
    </site>
</distributionManagement>
```
Where `Site-repo` is the name of the raw repository that must exist in Nexus to succeed.

Then, you can deploy the site as follows:

```groovy
mvn.useRepositoryCredentials([id: 'ces', credentialsId: 'nexusSystemUserCredential'])
mvn.deploySiteToNexus()
```

Where
 
* the `id` parameter must match the one specified in the `pom.xml`(`ces` in the example above), 
* the nexus username and password/access token are passed as jenkins username and password credential 
  (`nexusSystemUserCredential`).
* there is no difference between Nexus 2 and Nexus 3 regarding site deployments.

For an example see [cloudogu/continuous-delivery-slides-example](https://github.com/cloudogu/continuous-delivery-slides-example/)

### Passing additional arguments

Another option for `deployToNexusRepositoryWithStaging()` and `deployToNexusRepository()` is to pass additional maven 
arguments to the deployment like so: `mvn.deployToNexusRepositoryWithStaging('-X')` (enables debug output).

## Maven Utilities

Available from both local Maven and Maven in Docker.

* `mvn.getVersion()`
* `mvn.getArtifactId()`
* `mvn.getGroupId()`
* `mvn.getMavenProperty('project.build.sourceEncoding')`

See [Maven](src/com/cloudogu/ces/cesbuildlib/MavenInDocker.groovy)

# Gradle

## Gradle Wrapper in Docker

It's also possible to use a GradleWrapper in a Docker Container. Here, the Docker container is responsible for
providing the JDK.

See [GradleWrapperInDocker](src/com/cloudogu/ces/cesbuildlib/GradleWrapperInDocker.groovy)

Example:
```groovy
String gradleDockerImage = 'openjdk:11.0.10-jdk'
Gradle gradlew = new GradleWrapperInDocker(this, gradleDockerImage)

stage('Build') {
    gradlew "clean build"
}
```

Since Oracle's announcement of shorter free JDK support, plenty of JDK images have appeared on public container image
registries, where `adoptopenjdk` is just one option. The choice is yours.

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

You can optionally pass `usernamePassword` (i.e. a String containing the ID that refers to the 
[Jenkins credentials](https://jenkins.io/doc/book/using/using-credentials/)) to `Git` during construction. 
These are then used for cloning and pushing.

Note that the username and passwort are processed by a shell. Special characters in username or password might cause 
errors like `Unterminated quoted string`. So it's best to use a long password that only contains letters and numbers 
for now.

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
* `git.commitMessage` -  Last commit message e.g. `Implements new functionality...` 
* `git.commitHash` -  e.g. `fb1c8820df462272011bca5fddbe6933e91d69ed`
* `git.commitHashShort` -  e.g. `fb1c882`
* `git.areChangesStagedForCommit()` - `true` if changes are staged for commit. If `false`, `git.commit()` will fail. 
* `git.repositoryUrl` -  e.g. `https://github.com/orga/repo.git`
* `git.gitHubRepositoryName` -  e.g. `orga/repo`
* Tags - Note that the git plugin might not fetch tags for all builds. Run `sh "git fetch --tags"` to make sure.
    * `git.tag` -  e.g. `1.0.0` or empty if not set
    * `git.isTag()` - is there a tag on the current commit?

### Changes to local repository

Note that most changing operations offer parameters to specify an author.
Theses parameters are optional. If not set the author of the last commit will be used as author and committer.
You can specify a different committer by setting the following fields:

* `git.committerName = 'the name'` 
* `git.committerEmail = 'an.em@i.l` 

It is recommended to set a different committer, so it's obvious those commits were done by Jenkins in the name of
the author. This behaviour is implemented by GitHub for example when committing via the Web UI.

* `git.checkout('branchname')`
* `git.checkoutOrCreate('branchname')` - Creates new Branch if it does not exist
* `git.add('.')`
* `git.commit('message', 'Author', 'Author@mail.server)`
* `git.commit('message')` - uses default author/committer (see above).
* `git.setTag('tag', 'message', 'Author', 'Author@mail.server)`
* `git.setTag('tag', 'message')` - uses default author/committer (see above).
* `git.fetch()`
* `git.pull()` - pulls, and in case of merge, uses default author/committer (see above).
* `git.pull('refspec')` - pulls specific refspec (e.g. `origin master`), and in case of merge, uses the name and email 
   of the last committer as author and committer.
* `git.pull('refspec', 'Author', 'Author@mail.server)`
* `git.merge('develop', 'Author', 'Author@mail.server)`
* `git.merge('develop')` - uses default author/committer (see above).
* `git.mergeFastForwardOnly('master')`

### Changes to remote repository

* `git.push('origin master')` - pushes origin
   **Note**: This always prepends `origin` if not present for historical reasonse (see #44). 
   That is, right now it is impossible to push other remotes.  
   This will change in the next major version of ces-build-lib.  
   This limitation does not apply to other remote-related operations such as `pull()`, `fetch()` and `pushAndPullOnFailure()`
   So it's recommended to explicitly mention the origin and not just the refsepc:
     * Do: `git.push('origin master')`
     * Don't: `git.push('master')` because this will no longer work in the next major version.
* `git.pushAndPullOnFailure('refspec')` - pushes and pulls if push failed e.g. because local and remote have diverged, 
   then tries pushing again

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
 * `String findIp()` returns the IP address in the current context: the docker host ip (when outside of a container) or 
    the ip of the container this is running in
 * `String findDockerHostIp()` returns  the IP address of the docker host. Should work both, if running inside or 
    outside a container 
 * `String findEnv(container)` returns the environment variables set within the docker container as string
 * `boolean isRunningInsideOfContainer()` return `true` if this step is executed inside a container, otherwise `false`
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

* `repoDigests()`: Returns the repo digests, a content addressable unique digest of an image that was pushed 
   to or pulled  from repositories.  
   If the image was built locally and not pushed, returns an empty list.  
   If the image was pulled from or pushed to a repo, returns a list containing one item.  
   If the image was pulled from or pushed to multiple repos, might also contain more than one digest.  
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
   If no version parameter is passed, the lib tries to query the server version by calling `docker version`.  
   This can be called in addition to mountDockerSocket(), when the "docker" CLI is required on the PATH.  
   
   For available versions see [here](https://download.docker.com/linux/static/stable/x86_64/).

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

# Dockerfile

The `Dockerfile` class provides functions to lint Dockerfiles. For example:

```groovy
stage('Lint') {
    Dockerfile dockerfile = new Dockerfile(this)
    dockerfile.lint() // Lint with default configuration
    dockerfile.lintWithConfig() // Use your own hadolint configuration with a .hadolint.yaml configuration file
}
```

The tool [hadolint](https://github.com/hadolint/hadolint) is used for linting. It has a lot of configuration parameters
which can be set by creating a `.hadolint.yaml` file in your working directory.
See https://github.com/hadolint/hadolint#configure

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
  sonarQube.timeoutInMinutes = 4

  if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
    unstable("Pipeline unstable due to SonarQube quality gate failure")
  }
}
```

Note that
 
* Calling `waitForQualityGateWebhookToBeCalled()` requires a WebHook to be setup in your SonarQube server (globally or 
  per project), that notifies Jenkins (url: `https://yourJenkinsInstance/sonarqube-webhook/`).  
  See [SonarQube Scanner for Jenkins](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner+for+Jenkins#AnalyzingwithSonarQubeScannerforJenkins-AnalyzinginaJenkinspipeline). 
* Jenkins will wait for the webhook with a default timeout of 2 minutes, for big projects this might be to short and can be configured with the `timeoutInMinutes` property.
* Calling `waitForQualityGateWebhookToBeCalled()` will only work when an analysis has been performed in the current job,
  i.e. `analyzeWith()` has been called and in conjuction with `sonarQubeEnv`.
* When used in conjunction with [SonarQubeCommunity/sonar-build-breaker](https://github.com/SonarQubeCommunity/sonar-build-breaker),
  `waitForQualityGateWebhookToBeCalled()` will fail your build, if quality gate is not passed.
* For now, `SonarQube` can only analyze using `Maven`. Extending this to use the plain SonarQube Runner in future, 
  should be easy, however.
  
## Branches

By default, the `SonarQube` legacy logic, of creating one project per branch in a Jenkins Multibranch Pipeline project.

A more convenient alternative is the paid-version-only [Branch Plugin](https://docs.sonarqube.org/display/PLUG/Branch+Plugin)
or the [sonarqube-community-branch-plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin), which has 
similar features but is difficult to install, not supported officially and does not allow for migration to the official 
branch plugin later on.

You can enable either branch plugins like so:

```groovy
sonarQube.isUsingBranchPlugin = true
sonarQube.analyzeWith(mvn)
```

The branch plugin is using `master` as integration branch, if you want use a different branch as `master` you have to use the `integrationBranch` parameter e.g.:

```groovy
def sonarQube = new SonarQube(this, [sonarQubeEnv: 'sonarQubeServerSetupInJenkins', integrationBranch: 'develop'])
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
  def sonarQube = new SonarCloud(this, [sonarQubeEnv: 'sonarcloud.io', sonarOrganization: 'YOUR_ID'])

  sonarQube.analyzeWith(new MavenInDocker(this, "3.5.0-jdk-8"))

  if (!sonarQube.waitForQualityGateWebhookToBeCalled()) {
    unstable("Pipeline unstable due to SonarCloud quality gate failure")
  }
```

Just like for ordinary SonarQube, you have to setup a webhook in SonarCloud for `waitForQualityGateWebhookToBeCalled()` to work (see [above](#a-complete-example)).

If you want SonarCloud to decorate your Pull Requests, you will have to 

* GitHub: Install the [SonarCloud Application for GitHub](https://sonarcloud.io/documentation/integrations/github/) into your GitHub organization or account.
* BitBucket: Install the [SonarCloud add-on for Bitbucket Cloud](https://sonarcloud.io/documentation/integrations/bitbucketcloud/) into your BitBucket team or account.  
  Note that ces-build-lib supports only Git repos for now. No mercurial/hg, sorry.

See also [Pull Request analysis](https://sonarcloud.io/documentation/analysis/pull-request/).

Note that SonarCloud uses the Branch Plugin, so the first analysis has to be done differently, as described in [Branches](#branches).

## Pull Requests in SonarQube

As described above, SonarCloud can annotate PullRequests using the SonarCloud Application for GitHub. 
It is no longer possible to do this from a regular community edition SonarQube, as the 
[GitHub Plugin for SonarQube](https://docs.sonarqube.org/display/PLUG/GitHub+Plugin) is deprecated.

So a PR build is treated just like any other. That is, 

* without branch plugin: A new project using the `BRANCH_NAME` from env is created. 
* with Branch Plugin: A new branch is analysed using the `BRANCH_NAME` from env.

The Jenkins GitHub Plugin sets `BRANCH_NAME` to the PR Name, e.g. `PR-42`.


# Changelog
Provides the functionality to read changes of a specific version in a changelog that is 
based on the changelog format on https://keepachangelog.com/.

Note: The changelog will automatically be formatted. Characters like `"`, `'`, `\` will be removed. 
      A `\n` will be replaced with `\\n`. This is done to make it possible to pass this string to a json 
      struct as a value.

Example: 

```groovy
Changelog changelog = new Changelog(this)

stage('Changelog') {
  String changes = changelog.getChangesForVersion('v1.0.0')
  // ...
}
```
## changelogFileName
You can optionally pass the path to the changelog file if it is located somewhere else than in the root path or 
if the file name is not `CHANGELOG.md`.

Example: 

```groovy
Changelog changelog = new Changelog(this, 'myNewChangelog.md')

stage('Changelog') {
  String changes = changelog.getChangesForVersion('v1.0.0')
  // ...
}
```

# GitHub
Provides the functionality to do changes on a github repository such as creating a new release.

Example: 

```groovy
Git git = new Git(this)
GitHub github = new GitHub(this, git)

stage('Github') {
  github.createRelease('v1.1.1', 'Changes for version v1.1.1')
}
```

* `github.createRelease(releaseVersion, changes [, productionBranch])` - Creates a release on github. Returns the GitHub Release-ID.
   * Use the `releaseVersion` (String) as name and tag.
   * Use the `changes` (String) as body of the release.
   * Optionally, use `productionBranch` (String) as the name of the production release branch. This defaults to `master`.
* `github.createReleaseWithChangelog(releaseVersion, changelog [, productionBranch])` - Creates a release on github. Returns the GitHub Release-ID.
   * Use the `releaseVersion` (String) as name and tag.
   * Use the `changelog` (Changelog) to extract the changes out of a changelog and add them to the body of the release.
   * Optionally, use `productionBranch` (String) as the name of the production release branch. This defaults to `master`.
* `github.addReleaseAsset(releaseId, filePath)`
  * The `releaseId` (String) is the unique identifier of a release in the github API. Can be obtained as return value of `createReleaseWithChangelog` or `createRelease`.
  * The `filePath` specifies the path to the file which should be uploaded.
* `pushPagesBranch('folderToPush', 'commit Message')` - Commits and pushes a folder to the `gh-pages` branch of 
   the current repo. Can be used to conveniently deliver websites. See https://pages.github.com. Note:
   * Uses the name and email of the last committer as author and committer.
   * the `gh-pages` branch is temporarily checked out to the `.gh-pages` folder.
   * Don't forget to create a git object with credentials.
   * Optional: You can deploy to a sub folder of your GitHub Pages branch using a third parameter
   * Examples:
      * [cloudogu/continuous-delivery-slides](https://github.com/cloudogu/continuous-delivery-slides/)
      * [cloudogu/k8s-security-3-things](https://github.com/cloudogu/k8s-security-3-things)
   * See also [Cloudogu Blog: Continuous Delivery with reveal.js](https://cloudogu.com/en/blog/continuous-delivery-with-revealjs) 


# GitFlow

A wrapper class around the Git class to simplify the use of the git flow branching model.

Example: 

```groovy
Git git = new Git(this)
git.committerName = 'jenkins'
git.committerEmail = 'jenkins@your.org'
GitFlow gitflow = new GitFlow(this, git)

stage('Gitflow') {
  if (gitflow.isReleaseBranch()){
    gitflow.finishRelease(git.getSimpleBranchName())
  }
}
```

* `gitflow.isReleaseBranch()` - Checks if the currently checked out branch is a gitflow release branch.
* `gitflow.finishRelease(releaseVersion [, productionBranch])` - Finishes a git release by merging into develop and production release branch (default: "master").
   * Use the `releaseVersion` (String) as the name of the new git release.
   * Optionally, use `productionBranch` (String) as the name of the production release branch. This defaults to `master`.
   
# SCM-Manager

Provides the functionality to handle pull requests on a SCMManager repository.

You need to pass `usernamePassword` (i.e. a String containing the ID that refers to the
[Jenkins credentials](https://jenkins.io/doc/book/using/using-credentials/)) to `SCMManager` during construction.
These are then used for handling the pull requests.

```groovy
SCMManager scmm = new SCMManager(this, 'ourCredentials')
```

Set the repository url through the `repositoryUrl` property like so:

```groovy
SCMManager scmm = new SCMManager(this, 'https://hostname/scm', 'ourCredentials')
```

## Pull Requests

Each method requires a `repository` parameter, a String containing namespace and name, e.g. `cloudogu/ces-build-lib`.

* `scmm.searchPullRequestIdByTitle(repository, title)` - Returns a pull request ID by title, or empty, if not present.
    * Use the `repository` (String) as the GitOps repository
    * Use the `title` (String) as the title of the pull request in question.
    * This methods requires the `readJSON()` step from the 
      [Pipeline Utility Steps plugin](https://plugins.jenkins.io/pipeline-utility-steps/).
* `scmm.createPullRequest(repository, source, target, title, description)` - Creates a pull request, or empty, if not present.
    * Use the `repository` (String) as the GitOps repository
    * Use the `source` (String) as the source branch of the pull request.
    * Use the `target` (String) as the target branch of the pull request.
    * Use the `title` (String) as the title of the pull request.
    * Use the `description` (String) as the description of the pull request.
* `scmm.updatePullRequest(repository, pullRequestId, title, description)` - Updates the pull request.
    * Use the `repository` (String) as the GitOps repository
    * Use the `pullRequestId` (String) as the ID of the pull request.
    * Use the `title` (String) as the title of the pull request.
    * Use the `description` (String) as the description of the pull request.
* `scmm.createOrUpdatePullRequest(repository, source, target, title, description)` - Creates a pull request if no PR is found or updates the existing one.
    * Use the `repository` (String) as the GitOps repository
    * Use the `source` (String) as the source branch of the pull request.
    * Use the `target` (String) as the target branch of the pull request.
    * Use the `title` (String) as the title of the pull request.
    * Use the `description` (String) as the description of the pull request.
* `scmm.addComment(repository, pullRequestId, comment)` - Adds a comment to a pull request.
    * Use the `repository` (String) as the GitOps repository
    * Use the `pullRequestId` (String) as the ID of the pull request.
    * Use the `comment` (String) as the comment to add to the pull request.

Example:

```groovy
def scmm = new SCMManager(this, 'https://your.ecosystem.com/scm', scmManagerCredentials)

def pullRequestId = scmm.createPullRequest('cloudogu/ces-build-lib', 'feature/abc', 'develop', 'My title', 'My description')
pullRequestId = scmm.searchPullRequestIdByTitle('cloudogu/ces-build-lib', 'My title')
scmm.updatePullRequest('cloudogu/ces-build-lib', pullRequestId, 'My new title', 'My new description')
scmm.addComment('cloudogu/ces-build-lib', pullRequestId, 'A comment')
```

# HttpClient

`HttpClient` provides a simple `curl` frontend for groovy. 

* Not surprisingly, it requires `curl` on the jenkins agents.
* If you need to authenticate, you can create a `HttpClient` with optional credentials ID (`usernamePassword` credentials)
* `HttpClient` provides `get()`, `put()` and `post()` methods 
* All methods have the same signature, e.g.  
  `http.get(url, contentType = '', data = '')`
   * `url` (String) 
   * optional `contentType` (String) - set as acceptHeader in the request 
   * optional `data` (Object) - sent in the body of the request
* If successful, all methods return the same data structure a map of
  * `httpCode` - as string containing the http status code
  * `headers` - a map containing the response headers, e.g. `[ location: 'http://url' ]`
  * `body` - an optional string containing the body of the response  
* In case of an error (Connection refused, Could not resolve host, etc.) an exception is thrown which fails the build
  right away. If you don't want the build to fail, wrap the call in a `try`/`catch` block.

Example:

```groovy
HttpClient http = new HttpClient(scriptMock, 'myCredentialID')

// Simplest example
echo http.get('http://url')

// POSTing data
def dataJson = JsonOutput.toJson([
    comment: comment
])
def response = http.post('http://url/comments"', 'application/json', dataJson)

if (response.status == '201' && response.content-type == 'application/json') {
    def json = readJSON text: response.body
    echo json.count
}
```

# K3d

`K3d` provides functions to set up and administer a local k3s cluster in Docker.

Example:

```groovy
K3d k3d = new K3d(this, env.WORKSPACE, env.PATH)

try {
    stage('Set up k3d cluster') {
        k3d.startK3d()
    }

    stage('Do something with your cluster') {
        k3d.kubectl("get nodes")
    }
    stage('Apply your Helm chart') {
        k3d.helm("install path/to/your/chart")
    }

    stage('build and push development artefact') {
        String myCurrentArtefactVersion = "yourTag-1.2.3-dev"
        imageName = k3d.buildAndPushToLocalRegistry("your/image", myCurrentArtefactVersion)
        // your image name may look like this: k3d-citest-123456/your/image:yourTag-1.2.3-dev
        // the image name can be applied to your cluster as usual, f. i. with k3d.kubectl() with a customized K8s resource 
    }

    stage('install resources and wait for them') {
        imageName = "registry.cloudogu.com/official/my-dogu-name:1.0.0"
        k3d.installDogu("my-dogu-name", imageName, myDoguResourceYamlFile)

        k3d.waitForDeploymentRollout("my-dogu-name", 300, 5)
    }

    stage('install a dependent dogu by applying a dogu resource') {
        k3d.applyDoguResource("my-dependency", "nyNamespace", "10.0.0-1")
        k3d.waitForDeploymentRollout("my-dependency", 300, 5)
    }

} catch (Exception ignored) {
    // in case of a failed build collect dogus, resources and pod logs and archive them as log file on the build.
    k3d.collectAndArchiveLogs()
    throw e
} finally {
    stage('Remove k3d cluster') {
        k3d.deleteK3d()
    }
}
```

# DoguRegistry

`DoguRegistry` provides functions to easily push dogus and k8s components to a configured registry.

Example:

```groovy
DoguRegistry registry = new DoguRegistry(this)

// push dogu
registry.pushDogu()

// push k8s component
registry.pushK8sYaml("pathToMyK8sYaml.yaml", "k8s-dogu-operator", "mynamespace", "0.9.0")
```

# Bats

`Bats` provides functions to easily execute existing bats tests for a project. 

Example:

```groovy
Docker docker = new Docker(this)

stage('Bats Tests') {
    Bats bats = new Bats(this, docker)
    bats.checkAndExecuteTests()
}
```

# Makefile

`Makefile` provides function regarding the `Makefile` from the current directory.

Example:

```groovy
    Makefile makefile = new Makefile(this)
    String currentVersion = makefile.getVersion()
```

# Markdown

`Markdown` provides function regarding the `Markdown Files` from the projects docs directory  

```groovy
    Markdown markdown = new Markdown(this)
    markdown.check()
```


`markdown.check` executes the function defined in `Markdown`
running a container with the latest https://github.com/tcort/markdown-link-check image
and verifies that the links in the defined project directory are alive

Additionally, the markdown link checker can be used with a specific version (default: stable).

```groovy
    Markdown markdown = new Markdown(this, "3.11.0")
    markdown.check()
```

### DockerLint (Deprecated)

Use Dockerfile.lint() instead of lintDockerfile()!
See [Dockerfile](#dockerfile)

```groovy
lintDockerfile() // uses Dockerfile as default; optional parameter
```

See [lintDockerFile](vars/lintDockerfile.groovy)

### ShellCheck

```groovy
shellCheck() // search for all .sh files in folder and runs shellcheck
shellCheck(fileList) // fileList="a.sh b.sh" execute shellcheck on a custom list
```

See [shellCheck](vars/shellCheck.groovy)

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
For example, if running on `http(s)://server:port/jenkins`, `server` is returned.

## isBuildSuccessful

Returns true if the build is successful, i.e. not failed or unstable (yet).

## findVulnerabilitiesWithTrivy

Returns a list of vulnerabilities or an empty list if there are no vulnerabilities for the given severity.

`findVulnerabilitiesWithTrivy(trivyConfig as Map)`

```groovy
trivyConfig = [ 
    imageName: 'alpine:3.17.2', 
    severity: [ 'HIGH, CRITICAL' ], 
    trivyVersion: '0.41.0',
    additionalFlags: '--ignore-unfixed'
]
```

Here the only mandatory field is `imageName`. If no imageName was passed the function returns an empty list.

- **imageName** *(string)*: The name of the image to be scanned
- **severity** *(list of strings)*: If left blank all severities will be shown. If one or more are specified only these will be shown
  i.e. if 'HIGH' is passed then only vulnerabilities with the 'HIGH' score are shown
- **trivyVersion** *(string)*: The version of the trivy image
- **additionalFlags** *(string)*: Additional flags for trivy, e.g. `--ignore-unfixed`

### Simple examples

```groovy
node {
    stage('Scan Vulns') {
        def vulns = findVulnerabilitiesWithTrivy(imageName: 'alpine:3.17.2')
        if (vulns.size() > 0) {
            archiveArtifacts artifacts: '.trivy/trivyOutput.json'
            unstable "Found  ${vulns.size()} vulnerabilities in image. See vulns.json"
        }
    }
}
```

### Ignore / allowlist

If you want to ignore / allow certain vulnerabilities please use a .trivyignore file
Provide the file in your repo / directory where you run your job
e.g.:
```shell
.gitignore
Jenkinsfile
.trivyignore
```

[Offical documentation](https://aquasecurity.github.io/trivy/v0.41/docs/configuration/filtering/#by-finding-ids)
```ignorelang
# Accept the risk
CVE-2018-14618

# Accept the risk until 2023-01-01
CVE-2019-14697 exp:2023-01-01

# No impact in our settings
CVE-2019-1543

# Ignore misconfigurations
AVD-DS-0002

# Ignore secrets
generic-unwanted-rule
aws-account-id

```

If there are vulnerabilities the output looks as follows.

```json
{
  "SchemaVersion": 2,
  "ArtifactName": "alpine:3.17.2",
  "ArtifactType": "container_image",
  "Metadata": {
    "OS": {
      "Family": "alpine",
      "Name": "3.17.2"
    },
    "ImageID": "sha256:b2aa39c304c27b96c1fef0c06bee651ac9241d49c4fe34381cab8453f9a89c7d",
    "DiffIDs": [
      "sha256:7cd52847ad775a5ddc4b58326cf884beee34544296402c6292ed76474c686d39"
    ],
    "RepoTags": [
      "alpine:3.17.2"
    ],
    "RepoDigests": [
      "alpine@sha256:ff6bdca1701f3a8a67e328815ff2346b0e4067d32ec36b7992c1fdc001dc8517"
    ],
    "ImageConfig": {
      "architecture": "amd64",
      "container": "4ad3f57821a165b2174de22a9710123f0d35e5884dca772295c6ebe85f74fe57",
      "created": "2023-02-11T04:46:42.558343068Z",
      "docker_version": "20.10.12",
      "history": [
        {
          "created": "2023-02-11T04:46:42.449083344Z",
          "created_by": "/bin/sh -c #(nop) ADD file:40887ab7c06977737e63c215c9bd297c0c74de8d12d16ebdf1c3d40ac392f62d in / "
        },
        {
          "created": "2023-02-11T04:46:42.558343068Z",
          "created_by": "/bin/sh -c #(nop)  CMD [\"/bin/sh\"]",
          "empty_layer": true
        }
      ],
      "os": "linux",
      "rootfs": {
        "type": "layers",
        "diff_ids": [
          "sha256:7cd52847ad775a5ddc4b58326cf884beee34544296402c6292ed76474c686d39"
        ]
      },
      "config": {
        "Cmd": [
          "/bin/sh"
        ],
        "Env": [
          "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        ],
        "Image": "sha256:ba2beca50019d79fb31b12c08f3786c5a0621017a3e95a72f2f8b832f894a427"
      }
    }
  },
  "Results": [
    {
      "Target": "alpine:3.17.2 (alpine 3.17.2)",
      "Class": "os-pkgs",
      "Type": "alpine",
      "Vulnerabilities": [
        {
          "VulnerabilityID": "CVE-2023-0464",
          "PkgID": "libcrypto3@3.0.8-r0",
          "PkgName": "libcrypto3",
          "InstalledVersion": "3.0.8-r0",
          "FixedVersion": "3.0.8-r1",
          "Layer": {
            "DiffID": "sha256:7cd52847ad775a5ddc4b58326cf884beee34544296402c6292ed76474c686d39"
          },
          "SeveritySource": "nvd",
          "PrimaryURL": "https://avd.aquasec.com/nvd/cve-2023-0464",
          "DataSource": {
            "ID": "alpine",
            "Name": "Alpine Secdb",
            "URL": "https://secdb.alpinelinux.org/"
          },
          "Title": "Denial of service by excessive resource usage in verifying X509 policy constraints",
          "Description": "A security vulnerability has been identified in all supported versions of OpenSSL related to the verification of X.509 certificate chains that include policy constraints. Attackers may be able to exploit this vulnerability by creating a malicious certificate chain that triggers exponential use of computational resources, leading to a denial-of-service (DoS) attack on affected systems. Policy processing is disabled by default but can be enabled by passing the `-policy' argument to the command line utilities or by calling the `X509_VERIFY_PARAM_set1_policies()' function.",
          "Severity": "HIGH",
          "CweIDs": [
            "CWE-295"
          ],
          "CVSS": {
            "nvd": {
              "V3Vector": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H",
              "V3Score": 7.5
            },
            "redhat": {
              "V3Vector": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H",
              "V3Score": 5.9
            }
          },
          "References": [
            "https://access.redhat.com/security/cve/CVE-2023-0464",
            "https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-0464",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=2017771e2db3e2b96f89bbe8766c3209f6a99545",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=2dcd4f1e3115f38cefa43e3efbe9b801c27e642e",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=879f7080d7e141f415c79eaa3a8ac4a3dad0348b",
            "https://git.openssl.org/gitweb/?p=openssl.git;a=commitdiff;h=959c59c7a0164117e7f8366466a32bb1f8d77ff1",
            "https://nvd.nist.gov/vuln/detail/CVE-2023-0464",
            "https://ubuntu.com/security/notices/USN-6039-1",
            "https://www.cve.org/CVERecord?id=CVE-2023-0464",
            "https://www.openssl.org/news/secadv/20230322.txt"
          ],
          "PublishedDate": "2023-03-22T17:15:00Z",
          "LastModifiedDate": "2023-03-29T19:37:00Z"
        }
      ]
    }
  ]
}
```

# Examples
  * This library is built using itself! See [Jenkinsfile](Jenkinsfile)
  * [cloudugo/cas](https://github.com/cloudogu/cas)
  * [cloudogu/command-bus](https://github.com/cloudogu/command-bus)
  * [cloudogu/continuous-delivery-slides-example](https://github.com/cloudogu/continuous-delivery-slides-example/)
