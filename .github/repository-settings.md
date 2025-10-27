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

### Repository secrets

- `COPILOT_ASSIGNING_PAT` - owned by [@trask](https://github.com/trask)
- `GPG_PASSWORD` - stored in OpenTelemetry-Java 1Password
- `GPG_PRIVATE_KEY` - stored in OpenTelemetry-Java 1Password
- `NVD_API_KEY` - stored in OpenTelemetry-Java 1Password
  - Generated at https://nvd.nist.gov/developers/request-an-api-key
  - Key is associated with [@trask](https://github.com/trask)'s gmail address
- `SONATYPE_KEY` - owned by [@trask](https://github.com/trask)
- `SONATYPE_USER` - owned by [@trask](https://github.com/trask)

### Organization secrets

- `CODECOV_TOKEN`
- `DEVELOCITY_ACCESS_KEY` (scoped only to Java repos)
- `FOSSA_API_KEY`
- `OTELBOT_JAVA_CONTRIB_PRIVATE_KEY` (scoped only to this repo)
- `OTELBOT_PRIVATE_KEY`

### Organization variables

- `OSSF_SCORECARD_APP_ID`
- `OTELBOT_APP_ID`
