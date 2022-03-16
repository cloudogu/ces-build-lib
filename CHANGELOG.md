# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Changed
- Install kubectl on k3d setup automatically; #71
- Use workspace as k3d constructor parameter

### Removed
- Remove unused git credentials parameter in k3d constructor

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
