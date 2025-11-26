#!/usr/bin/env bash
#MISE description="Lint markdown files"
#MISE flag "--fix" help="Automatically fix issues"

set -e

if [ "${usage_fix}" = "true" ]; then
  markdownlint-cli2 --fix "**/*.md" "#**/build" "#**/node_modules" "#CHANGELOG.md" "#ibm-mq-metrics/docs/metrics.md" "#.github/pull_request_template.md"
else
  markdownlint-cli2 "**/*.md" "#**/build" "#**/node_modules" "#CHANGELOG.md" "#ibm-mq-metrics/docs/metrics.md" "#.github/pull_request_template.md"
fi
