# Repository settings

(In addition
to https://github.com/open-telemetry/community/blob/main/docs/how-to-configure-new-repository.md)

## General > Pull Requests

* Allow squash merging > Default to pull request title and description

* Automatically delete head branches: CHECKED

  So that bot PR branches will be deleted.

## Actions > General

* Fork pull request workflows from outside collaborators:
  "Require approval for first-time contributors who are new to GitHub"

  To reduce friction for new contributors
  (the default is "Require approval for first-time contributors").

## Branch protections

(In addition
to https://github.com/open-telemetry/community/blob/main/docs/how-to-configure-new-repository.md)

### `main` and `release/*`

* Require branches to be up to date before merging: UNCHECKED

  PR jobs take too long, and leaving this unchecked has not been a significant problem.

* Status checks that are required:

  * EasyCLA
  * required-status-check

### `dependabot/**/*`

* Require status checks to pass before merging: unchecked

  So that dependabot can rebase its PR branches

* Allow force pushes > Everyone

  So that dependabot can rebase its PR branches

* Allow deletions: CHECKED

  So that dependabot PR branches can be deleted

### `**/**`

* Status checks that are required:

  EasyCLA

* Allow deletions: CHECKED

  So that automation PR branches can be deleted
