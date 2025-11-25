#!/usr/bin/env bash
#MISE description="Lint markdown files"
#MISE flag "--fix" help="Automatically fix issues"

set -e

if [ "${usage_fix}" = "true" ]; then
  opt_fix="--fix"
fi

markdownlint-cli2 ${opt_fix} "**/*.md" "#**/build" "#**/node_modules" "#CHANGELOG.md" "#ibm-mq-metrics/docs/metrics.md" "#.github/pull_request_template.md"
