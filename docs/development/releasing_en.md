# Releasing ces-build-lib

The ces-build-lib is released via git-flow:
- Review and merge your PR into the develop branch
- Start git-flow release on develop branch, e.g. `git flow release start 1.51.0`
- Update CHANGELOG.md and pom.xml; commit
- Finish git-flow release, e.g. `git flow release finish -s 1.51.0`
- Push branches to remote, e.g. `git push origin develop --tags` and `git push origin main`
- Add new release on Github: https://github.com/cloudogu/ces-build-lib/releases