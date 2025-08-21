# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased]
## [4.3.0](https://github.com/cloudogu/ces-build-lib/releases/tag/4.3.0) - 2025-08-21
### Changed
- Updates the BATS shell test image to 1.12 which supports the `--report-formatter` switch

## [4.2.0](https://github.com/cloudogu/ces-build-lib/releases/tag/4.2.0) - 2025-04-02
### Changed
- Removes the default legacy behavior which created a new project in SonarQube for every analyzed branch.
This change uses the [community-branch-plugin](https://github.com/mc1arke/sonarqube-community-branch-plugin) as default
to create a project for the single artifact id. In this project all analyses of branches and pull requests will be stored.

### Fixed
- [#147] Use dogu v2 api for applying dogus in k3d.

## [4.1.1](https://github.com/cloudogu/ces-build-lib/releases/tag/4.1.1) - 2025-03-11
### Fixed
- [#143] Fix IllegalAccessError when using `GradleWrapperInDocker(..)`

## [4.1.0](https://github.com/cloudogu/ces-build-lib/releases/tag/4.1.0) - 2025-02-19
### Added
- Add checkChangelog function to make sure your changelog has been updated on an open PR
- Add checkReleaseNotes function to make sure your release notes have been updated on an open PR

## [4.0.1](https://github.com/cloudogu/ces-build-lib/releases/tag/4.0.1) - 2025-01-07
### Fixed
- Archive Trivy scan artifacts correctly

## [4.0.0](https://github.com/cloudogu/ces-build-lib/releases/tag/4.0.0) - 2025-01-07
### Added
- Add Trivy class for scanning container images with Trivy
  - Combines the functionality of the findVulnerabilitiesWithTrivy function and the Trivy class of the dogu-build-lib

### Deprecated
- findVulnerabilitiesWithTrivy function is deprecated now. Please use the new Trivy class.

### Changed
- [#140] Update Maven-Build-Dependencies 
  - JUnit 5
  - Groovy 2.5
  - Maven 3.9.9
    - Compiler-Target: Java 11

## [3.1.0](https://github.com/cloudogu/ces-build-lib/releases/tag/3.1.0) - 2024-11-25
### Added
- [#137] function to determine PreRelease Branch

## [3.0.0](https://github.com/cloudogu/ces-build-lib/releases/tag/3.0.0) - 2024-10-25
### Changed
- [#134] **Breaking**: Change container registry secret from `k8s-dogu-operator-docker-registry` to `ces-container-registries`.
This is necessary to support multiple registry configurations.

## [2.5.0](https://github.com/cloudogu/ces-build-lib/releases/tag/2.5.0) - 2024-10-14

### Added
- Custom maven image support
- Registry credentials for maven and gradle
- Additional Docker run args 

### Changed
- `Git.pull()` uses rebase strategy to avoid error `fatal: Need to specify how to reconcile divergent branches.`
- Updated Trivy version

## [2.4.0](https://github.com/cloudogu/ces-build-lib/releases/tag/2.4.0) - 2024-09-18
### Changed
- Relicense to AGPL-3.0-only

## [2.3.0](https://github.com/cloudogu/ces-build-lib/releases/tag/2.3.0) - 2024-09-11
### Changed
- change `k3d.configureComponents` so that `null` values disable single components so they won't be installed.
This is useful if default components like the blueprint-operator have to be disabled.
  - Example:
`k3d.configureComponents(["k8s-minio": ["version": "latest", "helmRepositoryNamespace": "k8s"], "k8s-blueprint-operator": null])`


## [2.2.1](https://github.com/cloudogu/ces-build-lib/releases/tag/2.2.1) - 2024-05-16
### Fixed
- [#125] K3d setup waits now until all dogus are rolled out and the setup is really done.

## [2.2.0](https://github.com/cloudogu/ces-build-lib/releases/tag/2.2.0) - 2024-05-02
### Added
- `Maven.useMirrors([name: 'maven-proxy', mirrorOf: 'central', url: 'https://maven.example.org'])`

## [2.1.0](https://github.com/cloudogu/ces-build-lib/releases/tag/2.1.0) - 2024-01-29
### Fixed
- [#120] K3d cluster is now able to run system calls used in go 1.21;
  - See https://github.com/docker-library/golang/issues/467

### Changed
- [#120] Updated k3d to `5.6.0`
- [#120] Updated k3s image to `rancher/k3s:v1.28.5-k3s1`
- Updated yq to `4.40.5`
- Fix yq output when using JSON-input from dogu.json; #122 

## [2.0.1](https://github.com/cloudogu/ces-build-lib/releases/tag/2.0.1) - 2023-12-11
### Fixed
- Add missing values.yaml as argument to helm install of k8s-ces-setup
- Add missing nginx-static to default setup.json

## [2.0.0](https://github.com/cloudogu/ces-build-lib/releases/tag/2.0.0) - 2023-12-08
### Changed
- Use Helm to install and configure k8s-ces-setup in k3d; #117

## [1.68.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.68.0) - 2023-11-30
### Added
- Add Helm installation with `k3d.installHelm()`; #115

## [1.67.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.67.0) - 2023-09-04
### Changed
- Switch to hadolint Dockerfile linter; #111

### Added
- Add Dockerfile class with new linting functions; #111
  - `lint()` lints with default hadolint config parameters. Only fails on errors
  - `lintWithConfig()` lets you specify the hadolint configuration

### Deprecated
- `lintDockerfile()` function should be replaced by the Dockerfile `lint()` function

## [1.66.1](https://github.com/cloudogu/ces-build-lib/releases/tag/1.66.1) - 2023-09-04
### Fixed
- Split helm-repo-config in separate values #113

## [1.66.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.66.0) - 2023-08-21
### Added
- Add helm-repo-config to k3d-cluster #109.

## [1.65.1](https://github.com/cloudogu/ces-build-lib/releases/tag/1.65.1) - 2023-08-17

- `findVulnerabilitiesWithTrivy` schema bug fix
  - The trivy output scheme is now interpreted correctly
  - Added `additionalFlags` as parameter e.g. '--ingore-unfixed' can be used now

## [1.65.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.65.0) - 2023-06-06

### Changed
- `findVulnerabilitiesWithTrivy` #107
  - Switch from using `allowlist` param to built-in `.trivyignore` file. Advantage: More declarative.  
    Fewer things in Jenkinsfile. Local trivy scans pick up allowlist as well.
  - Updated Trivy default to 0.41.0 from 0.15.0.
    - Trivy 0.20.0 introduced a JSON schema (see [here](https://github.com/aquasecurity/trivy/discussions/1050))
    - `findVulnerabilitiesWithTrivy` code can now only parse the new one
    - `findVulnerabilitiesWithTrivy` returns the new schema 
  - These are somewhat breaking changes, which will likely not affect anyone. So we dared to make them. Make sure to
    - not use `allowlist`, if so migrate to `.trivyignore`
    - not pin the `trivyVersion`, or update to trivy >= `0.20.0`
    - if you parsed the result of `findVulnerabilitiesWithTrivy` make sure to migrate to new schema, 
      e.g. `VulnerabilityID` moved to `.Results[].Vulnerabilities[].VulnerabilityID`

## [1.64.2](https://github.com/cloudogu/ces-build-lib/releases/tag/1.64.2) - 2023-04-24
### Fixed
- [#104] A missing `@NonCPS` annotation caused an error when calling the `K3d` constructor.

## [1.64.1](https://github.com/cloudogu/ces-build-lib/releases/tag/1.64.1) - 2023-04-17
### Fixed
- HttpClient escapes now the credentials in curl command to support credentials with characters like `$`.

## [1.64.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.64.0) - 2023-04-11
### Added
- Add parameter to configure version for markdown link checker #100.

## [1.63.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.63.0) - 2023-02-16
### Fixed
- A bug with SonarCloud where an error was thrown because a private field was accessed (#99)

## [1.62.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.62.0) - 2023-01-30
### Added
- Function lintDockerfile to lint docker files #96.
- Function shellCheck to lint shell scripts #96.

## [1.61.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.61.0) - 2023-01-13
### Added
- Add output from `kubectl describe` in the summary of the k8s resources in k3d. #94
  - these resources will be collected afterwards:
    - configmap, deployment, ingress, ingressclass, persistentvolume, persistentvolumeclaim, pod, replicaset, secret, service, statefulset
    
## [1.60.1](https://github.com/cloudogu/ces-build-lib/releases/tag/1.60.1) - 2022-12-01
### Fixed
- Incorrect usage of parameters makes currently used feature of installing and performing a setup unusable.

## [1.60.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.60.0) - 2022-11-30
### Changed
- Split method to perform an update into a configuration and installation subpart, which can be called independently. #90

## [1.59.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.59.0) - 2022-11-28
### Added
- Function `collectAndArchiveLogs` to collect dogu and resource information to help debugging k3s Jenkins builds. #89 
- Function `applyDoguResource(String doguName, String doguNamespace, String doguVersion)` to apply a custom dogu 
  resource into the cluster. This effectively installs a dogu if it is available. #89 

## [1.58.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.58.0) - 2022-11-07
### Changed
- Push k8s yaml content via file reference #87

## [1.57.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.57.0) - 2022-10-06
### Added
- New class `Bats` providing methods to easily execute existing bats (Bash Automated Testing System) tests. #85

## [1.56.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.56.0) - 2022-08-25
### Added
- New class `DockerRegistry` providing methods to: #83
  - push a dogu (json)
  - push a k8s component (yaml)

## [1.55.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.55.0) - 2022-07-06
### Added
- Markdown Link checker #81 see `README.md#Link-checker`

## [1.54.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.54.0) - 2022-06-21
### Added
- A ces setup is can automatically be performed for a K3d CES instance #79

## [1.53.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.53.0) - 2022-04-20
## Changed
- Adapt the secret creation for the k3d cluster according to the new secrets required by the `k8s-dogu-operator` #77

## [1.52.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.52.0) - 2022-03-30
## Added
- Automatic k8s secret generation for k3d clusters when starting the cluster. The secret contains
the login data used for the dogu-registry; #75

## [1.51.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.51.0) - 2022-03-16
### Added
- Add k3d method `buildAndPushToLocalRegistry()` for cluster-local access of development images #71
    - see the README.md for updated usage tips
- Makefile class to extract the current version from the makefile #73

### Changed
- Install kubectl on k3d setup automatically; #71
- Use workspace as k3d constructor parameter
- Install local image registry during k3d setup #71

### Removed
- Remove unused git credentials parameter in k3d constructor
- Remove dependency to GitOps playground

## [1.50.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.50.0) - 2022-03-10
### Changed
- Install Gitops Playground and K3d into a subfolder for better CI experience
  - see the README.md for updated usage tips 

## [1.49.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.49.0) - 2022-03-02
### Added
- Add Kubernetes support by spinning-up a k3d cluster #67
  - see the README.md for further information

## [1.48.0](https://github.com/cloudogu/ces-build-lib/releases/tag/1.48.0) -  2021-09-16
### Added
- Add gpg class to perform gpp based task such as signing; #64
- Add option to upload artifacts to a GitHub release; #64

## v0.0.1 - v1.47.1 / previous versions

Up till version v1.47.1 there was no change log
