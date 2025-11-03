# Versioning and releasing

OpenTelemetry Java Contrib uses [SemVer standard](https://semver.org) for versioning of its artifacts.

The version is specified in [version.gradle.kts](version.gradle.kts).

## Snapshot builds

Every successful CI build of the main branch automatically executes `./gradlew publishToSonatype`
as the last step, which publishes a snapshot build to the
[Sonatype snapshot repository](https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/io/opentelemetry/contrib/).

## Release cadence

This repository roughly targets monthly minor releases from the `main` branch on the Friday after
the second Monday of the month (roughly a couple of days after the monthly minor release of
[opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)).

## Preparing a new major or minor release

* Check that [renovate has run](https://developer.mend.io/github/open-telemetry/opentelemetry-java-contrib)
  sometime in the past day and that all
  [renovate PRs](https://github.com/open-telemetry/opentelemetry-java-contrib/pulls/app%2Frenovate)
  have been merged.
  * Check that the OpenTelemetry SDK and Instrumentation versions have been updated to the latest release.
* Close the [release milestone](https://github.com/open-telemetry/opentelemetry-java-contrib/milestones)
  if there is one.
* Merge a pull request to `main` updating the `CHANGELOG.md`.
  * The heading for the unreleased entries should be `## Unreleased`.
  * Use `.github/scripts/draft-change-log-entries.sh` as a starting point for writing the change log.
* Run the [Prepare release branch workflow](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/prepare-release-branch.yml).
  * Press the "Run workflow" button, and leave the default branch `main` selected.
  * Review and merge the two pull requests that it creates
    (one is targeted to the release branch and one is targeted to `main`).

## Preparing a new patch release

All patch releases should include only bug-fixes, and must avoid adding/modifying the public APIs.

In general, patch releases are only made for regressions, security vulnerabilities, memory leaks
and deadlocks.

* Backport pull request(s) to the release branch.
  * Run the [Backport workflow](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/backport.yml).
  * Press the "Run workflow" button, then select the release branch from the dropdown list,
    e.g. `release/v1.9.x`, then enter the pull request number that you want to backport,
    then click the "Run workflow" button below that.
  * Review and merge the backport pull request that it generates.
* Merge a pull request to the release branch updating the `CHANGELOG.md`.
  * The heading for the unreleased entries should be `## Unreleased`.
* Run the [Prepare patch release workflow](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/prepare-patch-release.yml).
  * Press the "Run workflow" button, then select the release branch from the dropdown list,
    e.g. `release/v1.9.x`, and click the "Run workflow" button below that.
  * Review and merge the pull request that it creates for updating the version.

## Making the release

* Run the [Release workflow](https://github.com/open-telemetry/opentelemetry-java-contrib/actions/workflows/release.yml).
  * Press the "Run workflow" button, then select the release branch from the dropdown list,
    e.g. `release/v1.9.x`, and click the "Run workflow" button below that.
  * This workflow will:
    * Publish the artifacts to Maven Central
    * Generate [GitHub Artifact Attestations](https://docs.github.com/en/actions/security-for-github-actions/using-artifact-attestations) for the JMX Metrics and JMX Scraper jars
    * Publish a GitHub release with release notes based on the change log and with the jars attached
  * Review and merge the pull request that it creates for updating the change log in main
    (note that if this is not a patch release then the change log on main may already be up-to-date,
    in which case no pull request will be created).

## Credentials

Same as the core repo, see [opentelemetry-java/RELEASING.md#credentials](https://github.com/open-telemetry/opentelemetry-java/blob/main/RELEASING.md#credentials).
