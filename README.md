#![Cloudogu logo](https://cloudogu.com/images/logo.png)
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

The build are run inside the official maven containers from [Dockerhub](https://hub.docker.com/_/maven/)

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
Note that this mounts the docker socket into the container. Use this wisely. [Some people](https://dzone.com/articles/never-expose-docker-sockets-period) say, you shouldn't do this at all! 


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

Provides the default methods of the global docker variable provided by [docker plugin](https://github.com/jenkinsci/docker-workflow-plugin):
 
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
 * `build(image, args)`: Runs docker build to create and tag the specified image from a Dockerfile in the current directory.
    Additional args may be added, such as `'-f Dockerfile.other --pull --build-arg http_proxy=http://192.168.1.1:3128 .'`.
    Like docker build, args must end with the build context.  
    Example:
     ```groovy
     def dockerContainer = docker.build("image/name:1.0", "folderOfDockfile").run("-e HOME=${pwd()}")
     ```

## Docker Utilities

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

# Steps

## mailIfStatusChanged

Provides the functionality of the Jenkins Post-build Action "E-mail Notification".

```
catchError {
 // Stages and steps
}
mailIfStatusChanged('a@b.cd,123@xy.z')
```
See [mailIfStatusChanged](vars/mailIfStatusChanged.groovy)

# Examples
  * This library is built using itself! See [Jenkinsfile](Jenkinsfile)
  * [cloudugo/cas](https://github.com/cloudogu/cas)
  * [triologygmbh/command-bus](https://github.com/triologygmbh/command-bus)