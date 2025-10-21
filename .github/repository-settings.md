# Repository settings

This document describes any changes that have been made to the
settings in this repository outside the settings tracked in the
private admin repo.

## Merge queue for `main`

[The admin repo doesn't currently support tracking merge queue settings.]

- Require merge queue: CHECKED
  - Build concurrency: 5
  - Maximum pull requests to build: 5
  - Minimum pull requests to merge: 1, or after 5 minutes
  - Maximum pull requests to merge: 5
  - Only merge non-failing pull requests: CHECKED
  - Status check timeout: 60 minutes

  ## Secrets and variables > Actions

  - `DEVELOCITY_ACCESS_KEY` - stored in OpenTelemetry-Java 1Password
    - Generated at https://develocity.opentelemetry.io > My settings > Access keys
    - Format of env var is `develocity.opentelemetry.io=<access key>`,
      see [docs](https://docs.gradle.com/enterprise/gradle-plugin/#via_environment_variable)
