# Versioning and releasing

OpenTelemetry Java Contrib uses [SemVer standard](https://semver.org) for versioning of its artifacts.

Instead of manually specifying project version (and by extension the version of built artifacts)
in gradle build scripts, we use [nebula-release-plugin](https://github.com/nebula-plugins/nebula-release-plugin)
to calculate the current version based on git tags. This plugin looks for the latest tag of the form
`vX.Y.Z` on the current branch and calculates the current project version as `vX.Y.(Z+1)-SNAPSHOT`.

## Snapshot builds
Every successful CI build of the master branch automatically executes `./gradlew snapshot` as the last task.
This signals Nebula plugin to build and publish to
[Sonatype OSS snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/io/opentelemetry/)
next _minor_ release version. This means version `vX.(Y+1).0-SNAPSHOT`.

## Starting the Release

Before making the release, merge a PR to `main` updating the `CHANGELOG.md`.
You can use the script at `buildscripts/draft-change-log-entries.sh` to help create an initial draft.
Typically only end-user facing changes are included in the change log.

Open the [Release workflow](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/release-build.yml).

Press the "Run workflow" button, then select the release branch from the dropdown list,
e.g. `v1.9.x`, and click the "Run workflow" button below that.

This workflow will publish the artifacts to maven central and will publish a github release with the
javaagent jar attached and release notes based on the change log.

### Notifying other OpenTelemetry projects

When cutting a new release, the relevant integration tests for components in other opentelemetry projects need to be updated.

- OpenTelemetry Collector contrib JMX receiver - [Downloads latest version here](https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/receiver/jmxreceiver/integration_test.go)

## Patch Release

All patch releases should include only bug-fixes, and must avoid adding/modifying the public APIs.

In general, patch releases are only made for bug-fixes for the following types of issues:
* Regressions
* Memory leaks
* Deadlocks

Before making the release:

* Merge PR(s) containing the desired patches to the release branch
* Merge a PR to the release branch updating the `CHANGELOG.md`

Open the [Patch release workflow](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/patch-release-build.yml).

Press the "Run workflow" button, then select the release branch from the dropdown list,
e.g. `v1.9.x`, and click the "Run workflow" button below that.

This workflow will publish the artifacts to maven central and will publish a github release with
release notes based on the change log.
