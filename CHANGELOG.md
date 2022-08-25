# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
