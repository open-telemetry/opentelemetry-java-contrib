# Repository settings

Same
as [opentelemetry-java-instrumentation repository settings](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/.github/repository-settings.md#repository-settings),
except for

- The rules for `gh-pages` and `cloudfoundry` branches are not relevant in this repository.

and the enablement of merge queues below.

## Merge queue

Needs to be enabled using classic branch protection (instead of rule set)
because of our use of the classic branch protection "Restrict who can push to matching branches"
which otherwise will block the merge queue from merging to main.

### Restrict branch creation

- Additional exclusion for `gh-readonly-queue/main/pr-*`

### Classic branch protection for `main`

- Require merge queue: CHECKED
  - Build concurrency: 5
  - Maximum pull requests to build: 5
  - Minimum pull requests to merge: 1, or after 5 minutes
  - Maximum pull requests to merge: 5
  - Only merge non-failing pull requests: CHECKED
  - Status check timeout: 60 minutes
